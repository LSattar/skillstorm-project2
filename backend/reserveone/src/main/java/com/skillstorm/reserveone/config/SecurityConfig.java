package com.skillstorm.reserveone.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.skillstorm.reserveone.services.CustomOAuth2UserService;
import com.skillstorm.reserveone.services.CustomOidcUserService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${FRONTEND_URL:https://dnyc0q77vtas5.cloudfront.net}")
    private String frontendUrl;

    private static String safe(String s) {
        if (s == null)
            return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String normalizePath(String p, String contextPath) {
        if (p == null)
            return null;

        String out = p;

        // strip query if somehow present
        int q = out.indexOf('?');
        if (q >= 0)
            out = out.substring(0, q);

        // remove trailing slashes
        while (out.endsWith("/") && out.length() > 1) {
            out = out.substring(0, out.length() - 1);
        }

        // strip context path prefix if present
        if (contextPath != null && !contextPath.isBlank() && out.startsWith(contextPath)) {
            out = out.substring(contextPath.length());
            if (out.isEmpty())
                out = "/";
        }

        return out;
    }

    private static boolean isStripeWebhookPath(String raw, String contextPath) {
        String p = normalizePath(raw, contextPath);
        if (p == null)
            return false;

        // strict matches first
        if ("/webhooks/stripe".equals(p))
            return true;
        if ("/api/webhooks/stripe".equals(p))
            return true;

        // proxy/container weirdness fallback
        return p.contains("/webhooks/stripe");
    }

    private static boolean equalsAny(String value, String... candidates) {
        if (value == null)
            return false;
        for (String c : candidates) {
            if (value.equals(c))
                return true;
        }
        return false;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CustomOAuth2UserService oAuth2UserService,
            CustomOidcUserService oidcUserService,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        // Treat XHR/SPA JSON calls differently: return 401 instead of redirecting to
        // Google
        RequestMatcher apiRequest = request -> {
            String xrw = request.getHeader("X-Requested-With");
            if (xrw != null && "xmlhttprequest".equalsIgnoreCase(xrw))
                return true;
            String accept = request.getHeader("Accept");
            return accept != null && accept.toLowerCase().contains("application/json");
        };

        // ✅ Robust webhook matcher (POST only) that survives /api context-path and
        // proxy quirks
        RequestMatcher stripeWebhook = request -> {
            if (!HttpMethod.POST.matches(request.getMethod()))
                return false;

            String ctx = request.getContextPath(); // typically "/api" when server.servlet.context-path=/api
            String uri = request.getRequestURI(); // often "/api/webhooks/stripe"
            String sp = request.getServletPath(); // sometimes "/webhooks/stripe"
            String pi = request.getPathInfo(); // sometimes null

            return isStripeWebhookPath(uri, ctx)
                    || isStripeWebhookPath(sp, ctx)
                    || isStripeWebhookPath(pi, ctx);
        };

        // Health/actuator matcher
        RequestMatcher healthOrActuator = request -> {
            String uri = request.getRequestURI();
            String sp = request.getServletPath();
            String pi = request.getPathInfo();

            return equalsAny(uri, "/health", "/api/health")
                    || (uri != null && (uri.startsWith("/actuator") || uri.startsWith("/api/actuator")
                            || uri.startsWith("/health/") || uri.startsWith("/api/health/")))
                    || (sp != null && (sp.startsWith("/actuator") || sp.startsWith("/api/actuator")
                            || sp.startsWith("/health") || sp.startsWith("/api/health")))
                    || (pi != null && (pi.startsWith("/actuator") || pi.startsWith("/api/actuator")
                            || pi.startsWith("/health") || pi.startsWith("/api/health")));
        };

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> {
                    CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    csrfRepo.setCookiePath("/");
                    csrfRepo.setCookieName("XSRF-TOKEN");
                    csrfRepo.setHeaderName("X-XSRF-TOKEN");
                    csrfRepo.setCookieCustomizer(builder -> builder
                            .path("/")
                            .sameSite("None")
                            .secure(true));

                    csrf.csrfTokenRepository(csrfRepo);

                    // Angular friendly
                    csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());

                    // ✅ Belt-and-suspenders: ignore + also do not REQUIRE CSRF on those requests
                    csrf.ignoringRequestMatchers(stripeWebhook, healthOrActuator);

                    RequestMatcher requireCsrf = new AndRequestMatcher(
                            CsrfFilter.DEFAULT_CSRF_MATCHER,
                            request -> !stripeWebhook.matches(request) && !healthOrActuator.matches(request));
                    csrf.requireCsrfProtectionMatcher(requireCsrf);
                })

                // Ensure CSRF token cookie gets written when accessed (good for SPA)
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                            throws ServletException, IOException {
                        CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
                        if (token != null)
                            token.getToken();
                        chain.doFilter(req, res);
                    }
                }, CsrfFilter.class)

                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                apiRequest)
                        .accessDeniedHandler((req, res, e) -> {
                            HttpSession s = req.getSession(false);
                            String sessionId = (s == null) ? "null" : s.getId();

                            String xsrfCookie = null;
                            if (req.getCookies() != null) {
                                for (var c : req.getCookies()) {
                                    if ("XSRF-TOKEN".equals(c.getName()))
                                        xsrfCookie = c.getValue();
                                }
                            }
                            String xsrfHeader = req.getHeader("X-XSRF-TOKEN");

                            Object csrfAttr = req.getAttribute(CsrfToken.class.getName());
                            String csrfExpected = null;
                            if (csrfAttr instanceof CsrfToken t)
                                csrfExpected = t.getToken();

                            Authentication a = SecurityContextHolder.getContext().getAuthentication();
                            String user = (a == null) ? "null" : a.getName();
                            String auths = (a == null) ? "null"
                                    : a.getAuthorities().stream()
                                            .map(ga -> ga.getAuthority())
                                            .collect(Collectors.joining(","));

                            // extra debug fields for proxy/context-path truth
                            String ctx = req.getContextPath();
                            String sp = req.getServletPath();
                            String pi = req.getPathInfo();

                            res.setStatus(HttpStatus.FORBIDDEN.value());
                            res.setContentType("application/json");

                            String body = """
                                    {
                                      "error": "FORBIDDEN",
                                      "path": "%s",
                                      "contextPath": "%s",
                                      "servletPath": "%s",
                                      "pathInfo": "%s",
                                      "method": "%s",
                                      "sessionId": "%s",
                                      "xsrfCookie": "%s",
                                      "xsrfHeader": "%s",
                                      "csrfExpected": "%s",
                                      "principal": "%s",
                                      "authorities": "%s",
                                      "exception": "%s"
                                    }
                                    """.formatted(
                                    safe(req.getRequestURI()),
                                    safe(ctx),
                                    safe(sp),
                                    safe(pi),
                                    safe(req.getMethod()),
                                    safe(sessionId),
                                    safe(xsrfCookie),
                                    safe(xsrfHeader),
                                    safe(csrfExpected),
                                    safe(user),
                                    safe(auths),
                                    safe(e.getClass().getSimpleName()));

                            res.getWriter().write(body);
                            res.setHeader("X-Security-Build", "csrf-fix-2026-01-19-0201");

                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // health/actuator
                        .requestMatchers("/health", "/health/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // OAuth2 handshake endpoints
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()

                        // SPA bootstrap
                        .requestMatchers(HttpMethod.GET, "/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/me").permitAll()

                        // ✅ webhook public (both forms kept for safety)
                        .requestMatchers(HttpMethod.POST, "/webhooks/stripe").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/stripe").permitAll()

                        // Public endpoints
                        .requestMatchers(HttpMethod.GET, "/rooms/available").permitAll()
                        .requestMatchers(HttpMethod.GET, "/rooms/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/rooms/hotel/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/hotels").permitAll()
                        .requestMatchers(HttpMethod.GET, "/hotels/*").permitAll()

                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(authorization -> {
                            DefaultOAuth2AuthorizationRequestResolver defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                                    clientRegistrationRepository, "/oauth2/authorization");

                            OAuth2AuthorizationRequestResolver resolver = new OAuth2AuthorizationRequestResolver() {
                                @Override
                                public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                                    OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
                                    return customizePromptIfGoogle(request, req);
                                }

                                @Override
                                public OAuth2AuthorizationRequest resolve(HttpServletRequest request,
                                        String clientRegistrationId) {
                                    OAuth2AuthorizationRequest req = defaultResolver.resolve(request,
                                            clientRegistrationId);
                                    return customizePromptIfGoogle(request, req);
                                }

                                private OAuth2AuthorizationRequest customizePromptIfGoogle(
                                        HttpServletRequest request, OAuth2AuthorizationRequest req) {
                                    if (req == null)
                                        return null;

                                    String uri = request.getRequestURI();
                                    boolean isGoogle = (uri != null && uri.toLowerCase().endsWith("/google"));
                                    if (!isGoogle)
                                        return req;

                                    LinkedHashMap<String, Object> additional = new LinkedHashMap<>(
                                            req.getAdditionalParameters());
                                    additional.put("prompt", "select_account");

                                    return OAuth2AuthorizationRequest.from(req)
                                            .additionalParameters(additional)
                                            .build();
                                }
                            };

                            authorization.authorizationRequestResolver(resolver);
                        })
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService)
                                .userService(oAuth2UserService))
                        .successHandler(this::oauth2SuccessRedirect))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("SESSION", "XSRF-TOKEN")
                        .logoutSuccessHandler((req, res, auth2) -> res.setStatus(200)));

        return http.build();
    }

    private void oauth2SuccessRedirect(
            HttpServletRequest req,
            HttpServletResponse res,
            Authentication auth) throws IOException {

        String target = (frontendUrl == null || frontendUrl.isBlank())
                ? "http://localhost:4200/"
                : (frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/");

        res.setStatus(302);
        res.setHeader("Location", target);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of(
                frontendUrl,
                "http://localhost:4200",
                "http://127.0.0.1:4200"));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Referer",
                "Stripe-Signature"));

        config.setExposedHeaders(List.of("Set-Cookie", "XSRF-TOKEN"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
