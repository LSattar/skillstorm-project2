package com.skillstorm.reserveone.controllers;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class CsrfController {

     @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(HttpServletRequest req) {

        // Force token to be resolved for this request.
        // CookieCsrfTokenRepository will then write XSRF-TOKEN cookie.
        CsrfToken token = (CsrfToken) req.getAttribute(CsrfToken.class.getName());
        // CSRF tokens are per-client and must never be cached.

          String value = (token == null) ? "" : token.getToken();
          return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(Map.of("token", value));
    }
}



   
        

      

     
}
