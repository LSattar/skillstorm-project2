package com.skillstorm.fincen_project2_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Temporary security configuration for development.
 * All requests are permitted to allow application development
 * before authentication and authorization are enforced.
 */

@Configuration
// enableWebSecurity - Annotation // this one enables various web security
// functions, like basic auth, CORS, CSRF, etc.
public class SecurityConfig {

    /**
     * Be careful about doing research with spring security
     * many methods got deprecated in summer 2022
     * 
     * SecurityConfiguration class used to extend WebSecurityConfigureAdapter (this
     * is deprecated)
     * 
     * antMatchers used to be used instead of mvcMatchers
     * antMatchers are not deprecated but in general mvcMatchers is preferred
     * 
     * @param http
     * @return
     * @throws Exception
     */

    //Add each end point below

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * 
     * Chaining http object
     * 
     * http.httpBasic(httpBasic -> Customizer.withDefaults());
     * 
     * http.authorizeHttpRequests(auth -> {
     * auth.requestMatchers("/main/**").permitAll(). // PermitAll() No
     * authentication needed
     * .requestMatchers("/main/protected").authenticated()); => Needs authentication
     * })
     */

    /**
     * 
     * <Using the httpSecurity object to configure which endpoints require
     * authentication/authorization>
     * 
     * http.authorizeHttpRequests((authorizeHttpRequests) ->
     * authorizeHttpRequests
     * .mvcMatchers("/users/hello").permitAll() // allowing all access to
     * /users/hello/without authentication
     * );
     * 
     * return http.build();
     */

    /** 
    
    
    //injecting our CustomerUserDetailsService here for finally authenticating the incoming password against the one in the database
    
    private final CustomUserDetailsService service){
    this.service = service;
    }
    
    we need a bean for our password encoder -- using bcrypt here!
    @Bean
    PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
    }
    
    //last!!
    //we need to say HOW we're managing authentication
    // in our case, it's using bcrypt to handle passwords
    // we must feed in both the http object and the password encoder as parameters
    
    @Bean
    AuthenticationManager authManager(HttpSecurity http, PasswordEncoder passwordEncoder){
        // start building an auth object
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        // chain onto that auth object with the services we want to use
        // for us. that's our UserDetailsService object and our PasswordEncoder
        auth.userDetailsService(service).passwordEncoder(passwordEncoder);
        return auth.build();
    }
    
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http){
        http
            .csrf(csrf -> csrf.diable())
            .authroizeHttpRequests(auth -> auth
            
            )
    }
    
    
    //*/

}
