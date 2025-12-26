package com.skillstorm.reserveone.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.skillstorm.reserveone.services.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService oAuth2UserService) throws Exception {

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
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                // If you want logout to work without CSRF header, uncomment:
                                // .ignoringRequestMatchers("/logout")
                                )
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                                // For SPA/XHR calls, return 401 instead of redirecting to OAuth login.
                                .exceptionHandling(ex -> ex
                                                .defaultAuthenticationEntryPointFor(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                                                apiRequest))

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/error").permitAll()

                                                // OAuth2 handshake endpoints
                                                .requestMatchers("/oauth2/**", "/login/**").permitAll()

                                                // CSRF bootstrap for SPA
                                                .requestMatchers(HttpMethod.GET, "/csrf").permitAll()

                                                // Session check for SPA
                                                .requestMatchers(HttpMethod.GET, "/auth/me").permitAll()

                                                // Public endpoints
                                                .requestMatchers(HttpMethod.GET, "/rooms/available").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/rooms/*").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/rooms/hotel/*").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/hotels").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/hotels/*").permitAll()

                                                // Users
                                                .requestMatchers(HttpMethod.GET, "/users/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/users/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/users/*")
                                                .hasAnyRole("ADMIN", "BUSINESS_OWNER")
                                                .requestMatchers(HttpMethod.PATCH, "/users/*/status")
                                                .hasRole("ADMIN")

                                                // Bookings
                                                .requestMatchers(HttpMethod.POST, "/bookings")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/bookings/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/bookings/user/*")
                                                .hasAnyRole("GUEST", "EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/bookings/*")
                                                .hasAnyRole("EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/bookings/*")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/bookings")
                                                .hasAnyRole("EMPLOYEE", "MANAGER", "BUSINESS_OWNER", "ADMIN")

                                                // Rooms
                                                .requestMatchers(HttpMethod.POST, "/rooms")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/rooms/*")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/rooms/*")
                                                .hasAnyRole("BUSINESS_OWNER", "ADMIN")

                                                // Hotels
                                                .requestMatchers(HttpMethod.POST, "/hotels")
                                                .hasAnyRole("BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/hotels/*")
                                                .hasAnyRole("BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/hotels/*")
                                                .hasRole("ADMIN")

                                                .requestMatchers("/roles/**").hasRole("ADMIN")
                                                .requestMatchers("/reports/**")
                                                .hasAnyRole("MANAGER", "BUSINESS_OWNER", "ADMIN")
                                                .requestMatchers("/analytics/**").hasAnyRole("BUSINESS_OWNER", "ADMIN")

                                                .anyRequest().authenticated())

                                .oauth2Login(oauth -> oauth
                                                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService)))

                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                                                .logoutSuccessHandler((req, res, auth) -> res.setStatus(200)));

                return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOriginPatterns(List.of(
                                "http://localhost:4200",
                                "http://127.0.0.1:4200"));

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

                config.setAllowedHeaders(List.of(
                                "Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With", "Accept", "Origin",
                                "Referer"));

                config.setExposedHeaders(List.of("Set-Cookie", "XSRF-TOKEN"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}
