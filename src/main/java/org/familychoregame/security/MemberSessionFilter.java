package org.familychoregame.security;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.familychoregame.repository.GameRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/** Reject sessions whose member was deleted by a reset or archived. */
final class MemberSessionFilter extends OncePerRequestFilter {
    private final GameRepository repository;

    MemberSessionFilter(GameRepository repository) {
        this.repository = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedMember member
                && !repository.activeMemberExists(member.id())) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        chain.doFilter(request, response);
    }
}