package org.familychoregame.api;

import static org.familychoregame.api.ApiModels.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.familychoregame.api.ApiException;
import org.familychoregame.repository.GameRepository;
import org.familychoregame.security.AuthenticatedMember;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
    private static final int MAXIMUM_FAILED_LOGINS = 5;
    private static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(15);
    private final GameRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(GameRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/session")
    SessionInfo session(Authentication authentication, CsrfToken csrfToken) {
        csrfToken.getToken();
        var setupRequired = repository.family().isEmpty();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedMember member)) {
            return new SessionInfo(false, setupRequired, null, null, null);
        }
        return new SessionInfo(true, setupRequired, member.id(), member.displayName(), member.role());
    }

    @GetMapping("/login-members")
    List<LoginMember> loginMembers() {
        return repository.loginMembers();
    }

    @PostMapping("/login")
    SessionInfo login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
                      HttpServletResponse servletResponse) {
        var account = repository.authMember(request.memberId())
                .orElseThrow(() -> invalidLogin());
        var now = Instant.now();
        if (repository.loginLocked(account.id(), now)) throw loginLocked(account.id());
        if (!passwordEncoder.matches(request.pin(), account.pinHash())) {
            var locked = repository.recordFailedLogin(account.id(), now, MAXIMUM_FAILED_LOGINS,
                    now.plus(LOGIN_LOCK_DURATION));
            LOGGER.atWarn().addKeyValue("event", "login_failed")
                    .addKeyValue("member_id", account.id()).addKeyValue("account_locked", locked)
                    .log("Member login failed");
            if (locked) throw loginLocked(account.id());
            throw invalidLogin();
        }
        repository.clearFailedLogins(account.id());
        var principal = new AuthenticatedMember(account.id(), account.displayName(), account.role());
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.role())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, servletRequest, servletResponse);
        servletRequest.changeSessionId();
        LOGGER.atInfo().addKeyValue("event", "login_succeeded")
                .addKeyValue("member_id", account.id()).addKeyValue("role", account.role())
                .log("Member login succeeded");
        return new SessionInfo(true, false, principal.id(), principal.displayName(), principal.role());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var memberId = authentication != null && authentication.getPrincipal() instanceof AuthenticatedMember member
                ? member.id() : null;
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        LOGGER.atInfo().addKeyValue("event", "logout_succeeded").addKeyValue("member_id", memberId)
                .log("Member logged out");
    }

    private ApiException invalidLogin() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_login", "Invalid member or PIN");
    }

    private ApiException loginLocked(String memberId) {
        LOGGER.atWarn().addKeyValue("event", "login_blocked")
                .addKeyValue("member_id", memberId).log("Locked member login blocked");
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "too_many_login_attempts",
                "Too many login attempts. Try again later");
    }
}
