package com.college.voting.config;

import com.college.voting.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configure CSRF to store tokens in cookies, readable by frontend JS
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(
                    "/api/auth/login", "/api/auth/admin-login", "/api/auth/logout",
                    "/api/auth/otp/verify", "/api/auth/otp/resend"
                )
            )
            .addFilterAfter(new CsrfCookieFilter(), UsernamePasswordAuthenticationFilter.class)
            // Session Management
            .sessionManagement(session -> session
                // Concurrency control: max 1 session per student
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true)
            )
            // Exception handling: return 401 instead of redirecting to login page for APIs
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"Access Denied\"}");
                })
            )
            // Authorize requests
            .authorizeHttpRequests(auth -> auth
                // Public files and folders
                .requestMatchers(
                    "/", "/index.html", "/css/**", "/js/**", "/images/**", "/assets/**",
                    "/pages/student-login.html", "/pages/admin-login.html"
                ).permitAll()
                
                // Public REST auth and stats endpoints
                .requestMatchers(
                    "/api/auth/login", "/api/auth/admin-login", "/api/auth/status", "/api/auth/logout",
                    "/api/auth/otp/verify", "/api/auth/otp/resend", "/api/public/election-status"
                ).permitAll()

                // Static student and admin files are restricted to appropriate roles
                .requestMatchers("/pages/student-dashboard.html", "/pages/voting.html", "/pages/already-voted.html", "/pages/profile.html").hasRole("STUDENT")
                .requestMatchers("/pages/admin-dashboard.html", "/pages/manage-students.html", "/pages/manage-candidates.html", "/pages/election-settings.html", "/pages/results.html", "/pages/settings.html", "/pages/vote-records.html").hasRole("ADMIN")
                
                // Restricted Admin API paths
                .requestMatchers("/api/admin/**", "/api/election/**").hasRole("ADMIN")
                
                // REST Results endpoint (Admin only or logic check)
                .requestMatchers("/api/results/**").hasAnyRole("ADMIN", "STUDENT")
                
                // Restricted Student/Voting API paths
                .requestMatchers("/api/student/**", "/api/voting/**").hasRole("STUDENT")
                
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("{\"message\": \"Logged out successfully\"}");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}

class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // Force lazy token evaluation to write XSRF-TOKEN cookie
        }
        filterChain.doFilter(request, response);
    }
}
