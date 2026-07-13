package com.college.voting.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class OtpVerificationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        
        // Skip filtering for assets, css, js, login endpoints, etc.
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/images/") 
            || uri.startsWith("/assets/") || uri.equals("/") || uri.equals("/index.html")
            || uri.equals("/pages/student-login.html") || uri.equals("/pages/admin-login.html")
            || uri.equals("/api/auth/login") || uri.equals("/api/auth/admin-login") 
            || uri.equals("/api/auth/logout") || uri.equals("/api/auth/status")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() 
            && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))) {
            
            HttpSession session = httpRequest.getSession(false);
            boolean isOtpVerified = session != null && Boolean.TRUE.equals(session.getAttribute("OTP_VERIFIED"));

            // Student is not OTP verified
            if (!isOtpVerified) {
                // Allow OTP endpoints and OTP page
                if (uri.equals("/pages/otp-verification.html") 
                    || uri.startsWith("/api/auth/otp/verify") 
                    || uri.startsWith("/api/auth/otp/resend")) {
                    chain.doFilter(request, response);
                    return;
                }

                // If requesting an API, return 403 Forbidden with a JSON response indicating OTP required
                if (uri.startsWith("/api/")) {
                    httpResponse.setContentType("application/json");
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.getWriter().write("{\"error\": \"OTP_REQUIRED\", \"message\": \"OTP verification required\"}");
                    return;
                }

                // If requesting an HTML page, redirect to the OTP verification page
                httpResponse.sendRedirect("/pages/otp-verification.html");
                return;
            } else {
                // Student IS OTP verified. If they try to go back to OTP verification page, redirect to dashboard.
                if (uri.equals("/pages/otp-verification.html")) {
                    httpResponse.sendRedirect("/pages/student-dashboard.html");
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }
}
