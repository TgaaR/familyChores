package org.familychoregame.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.familychoregame.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataResetController {
    private final GameService service;

    public DataResetController(GameService service) {
        this.service = service;
    }

    @DeleteMapping("/api/v1/data")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clearAllData(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        service.clearAllData();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}