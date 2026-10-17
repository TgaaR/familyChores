package org.familychoregame.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/** Materialize Spring Security's deferred CSRF cookie when Swagger loads its configuration. */
final class SwaggerCsrfFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/swagger-ui")
                || request.getRequestURI().startsWith(request.getContextPath() + "/v3/api-docs")) {
            var token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) token.getToken();
        }
        chain.doFilter(request, response);
    }
}