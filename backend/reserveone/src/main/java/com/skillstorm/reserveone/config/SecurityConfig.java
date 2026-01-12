package com.skillstorm.reserveone.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.skillstorm.reserveone.services.CustomOAuth2UserService;
import com.skillstorm.reserveone.services.CustomOidcUserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Value("${FRONTEND_URL:https://dnyc0q77vtas5.cloudfront.net}")
        private String frontendUrl;

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
                        if (xrw != null && "xmlhttprequest".equalsIgnoreCase(xrw)) {
                                return true;
                        }
                        String accept = request.getHeader("Accept");
                        return accept != null && accept.toLowerCase().contains("application/json");
                };

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> {
                                        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository
                                                        .withHttpOnlyFalse();
                                        csrfRepo.setCookieCustomizer(builder -> builder
                                                        .path("/")
                                                        .sameSite("None")
                                                        .secure(true));
                                        csrf.csrfTokenRepository(csrfRepo);

                                        // Health/actuator must never require CSRF (ALB probes, uptime monitors)
                                        csrf.ignoringRequestMatchers(
                                                        "/api/health",
                                                        "/api/health/**",
                                                        "/api/actuator/**");

                                        // If you want logout to work without CSRF header, uncomment:
                                        // csrf.ignoringRequestMatchers("/api/logout");
                                })
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.IF_REQUIRED))
                                .exceptionHandling(ex -> ex
                                                .defaultAuthenticationEntryPointFor(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                                                apiRequest))
                                .authorizeHttpRequests(auth -> auth
                                                // Preflight
                                                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                                                .permitAll()
                                                .requestMatchers("/error").permitAll()

                                                // === Health checks (ALB / EB) ===
                                                .requestMatchers("/api/health", "/api/health/**").permitAll()
                                                .requestMatchers("/api/actuator/health", "/api/actuator/health/**")
                                                .permitAll()

                                                // OAuth2 handshake endpoints (with context-path, these are under /api)
                                                .requestMatchers("/api/oauth2/**", "/api/login/**").permitAll()

                                                // CSRF bootstrap for SPA
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/csrf")
                                                .permitAll()

                                                // Session check for SPA
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/auth/me")
                                                .permitAll()

                                                // Public endpoints
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/rooms/available")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/rooms/*")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/rooms/hotel/*")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/hotels")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/hotels/*")
                                                .permitAll()

                                                // Users
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/users/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                                                "/api/users/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/users/*")
                                                .hasAnyRole("ADMIN", "BUSINESS_OWNER")
                                                .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                                                "/api/users/*/status")
                                                .hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/users/search")
                                                .hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                                                "/api/users/*/roles")
                                                .hasRole("ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/users")
                                                .hasRole("ADMIN")

                                                // Bookings
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/bookings")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/bookings/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/bookings/user/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                                                "/api/bookings/*")
                                                .hasAnyRole("EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/bookings/*")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/bookings")
                                                .hasAnyRole("EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")

                                                // Rooms
                                                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/rooms")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                                                "/api/rooms/*")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/rooms/*")
                                                .hasAnyRole("BUSINESS_OWNER", "ADMIN")

                                                // Hotels
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/hotels")
                                                .hasAnyRole("BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.PATCH,
                                                                "/api/hotels/*")
                                                .hasAnyRole("BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/hotels/*")
                                                .hasRole("ADMIN")

                                                // Roles / Reports / Analytics
                                                .requestMatchers("/api/roles/**").hasRole("ADMIN")
                                                .requestMatchers("/api/reports/**")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers("/api/analytics/**")
                                                .hasAnyRole("BUSINESS_OWNER", "ADMIN")

                                                .anyRequest().authenticated())
                                .oauth2Login(oauth -> oauth
                                                .authorizationEndpoint(authorization -> {
                                                        DefaultOAuth2AuthorizationRequestResolver defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                                                                        clientRegistrationRepository,
                                                                        "/oauth2/authorization");

                                                        OAuth2AuthorizationRequestResolver resolver = new OAuth2AuthorizationRequestResolver() {
                                                                @Override
                                                                public OAuth2AuthorizationRequest resolve(
                                                                                HttpServletRequest request) {
                                                                        OAuth2AuthorizationRequest req = defaultResolver
                                                                                        .resolve(request);
                                                                        if (req == null)
                                                                                return null;
                                                                        String registrationId = extractRegistrationId(
                                                                                        request);
                                                                        return customizePrompt(registrationId, req);
                                                                }

                                                                @Override
                                                                public OAuth2AuthorizationRequest resolve(
                                                                                HttpServletRequest request,
                                                                                String clientRegistrationId) {
                                                                        OAuth2AuthorizationRequest req = defaultResolver
                                                                                        .resolve(request,
                                                                                                        clientRegistrationId);
                                                                        if (req == null)
                                                                                return null;
                                                                        return customizePrompt(clientRegistrationId,
                                                                                        req);
                                                                }

                                                                private String extractRegistrationId(
                                                                                HttpServletRequest request) {
                                                                        String uri = request.getRequestURI();
                                                                        int lastSlash = uri.lastIndexOf('/');
                                                                        return (lastSlash >= 0)
                                                                                        ? uri.substring(lastSlash + 1)
                                                                                        : uri;
                                                                }

                                                                private OAuth2AuthorizationRequest customizePrompt(
                                                                                String registrationId,
                                                                                OAuth2AuthorizationRequest req) {
                                                                        if (!"google".equalsIgnoreCase(registrationId))
                                                                                return req;

                                                                        LinkedHashMap<String, Object> additional = new LinkedHashMap<>(
                                                                                        req.getAdditionalParameters());
                                                                        additional.put("prompt", "select_account");

                                                                        return OAuth2AuthorizationRequest.from(req)
                                                                                        .additionalParameters(
                                                                                                        additional)
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
                                                .logoutUrl("/api/logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                                                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200)));

                return http.build();
        }

        private void oauth2SuccessRedirect(
                        HttpServletRequest req,
                        HttpServletResponse res,
                        org.springframework.security.core.Authentication auth) throws IOException, ServletException {

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
                                "Referer"));

                config.setExposedHeaders(List.of("Set-Cookie", "XSRF-TOKEN"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
