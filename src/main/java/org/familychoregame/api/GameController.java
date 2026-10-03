package org.familychoregame.api;

import static org.familychoregame.api.ApiModels.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.familychoregame.domain.Assignment;
import org.familychoregame.domain.RightRedemption;
import org.familychoregame.domain.FamilyMember;
import org.familychoregame.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GameController {
    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    @GetMapping("/bootstrap")
    Bootstrap bootstrap() { return service.bootstrap(); }

    @PostMapping("/families")
    @ResponseStatus(HttpStatus.CREATED)
    Bootstrap setupFamily(@Valid @RequestBody SetupFamilyRequest request) { return service.setupFamily(request); }

    @GetMapping("/weeks/{weekStart}")
    WeekBoard week(@PathVariable LocalDate weekStart) { return service.board(weekStart); }

    @PostMapping("/members")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.CREATED)
    FamilyMember addMember(@Valid @RequestBody AddMemberRequest request) { return service.addMember(request); }

    @DeleteMapping("/members/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void archiveMember(@PathVariable String id) { service.archiveMember(id); }

    @PostMapping("/assignments")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.CREATED)
    Assignment createAssignment(@Valid @RequestBody CreateAssignmentRequest request) {
        return service.createAssignment(request);
    }

    @DeleteMapping("/assignments/{id}")
    @PreAuthorize("hasRole('PARENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAssignment(@PathVariable String id) { service.deleteAssignment(id); }

    @PatchMapping("/assignments/{id}/completion")
    Assignment setCompletion(@PathVariable String id, @Valid @RequestBody CompletionRequest request) {
        return service.setCompletion(id, request);
    }

    @PostMapping("/right-redemptions")
    @ResponseStatus(HttpStatus.CREATED)
    RightRedemption redeemRight(@Valid @RequestBody RedeemRightRequest request) {
        return service.redeemRight(request);
    }
}
