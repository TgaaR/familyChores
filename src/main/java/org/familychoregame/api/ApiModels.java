package org.familychoregame.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.familychoregame.domain.Assignment;
import org.familychoregame.domain.Right;
import org.familychoregame.domain.RightRedemption;
import org.familychoregame.domain.Chore;
import org.familychoregame.domain.ChoreCategory;
import org.familychoregame.domain.FamilyMember;

public final class ApiModels {
    private ApiModels() {}

    public record AddMemberRequest(@NotBlank @Size(max = 24) String displayName,
                                   @NotBlank @Pattern(regexp = "PARENT|CHILD") String role,
                                   @NotBlank @Pattern(regexp = "\\d{4,6}") String pin) {}
    public record SetupMemberRequest(@NotBlank @Size(max = 24) String displayName,
                                     @NotBlank @Pattern(regexp = "PARENT|CHILD") String role,
                                     @NotBlank @Pattern(regexp = "\\d{4,6}") String pin) {}
    public record SetupFamilyRequest(@NotBlank @Size(max = 60) String familyName,
                                     @NotBlank @Pattern(regexp = "he|en") String defaultLanguage,
                                     @NotEmpty List<@jakarta.validation.Valid SetupMemberRequest> members) {}
    public record CreateAssignmentRequest(@NotBlank String requestId, @NotBlank String memberId,
                                          @NotBlank String choreId, @NotNull LocalDate assignmentDate) {}
    public record CompletionRequest(boolean completed, long version) {}
    public record RedeemRightRequest(@NotBlank String requestId, @NotBlank String memberId,
                                       @NotBlank String rightId) {}
    public record MemberScore(String memberId, int completedCount, int assignmentCount, int points) {}
    public record MemberBalance(String memberId, int earnedPoints, int spentPoints, int availablePoints) {}
    public record LoginRequest(@NotBlank String memberId,
                               @NotBlank @Pattern(regexp = "\\d{4,6}") String pin) {}
    public record LoginMember(String id, String displayName) {}
    public record SessionInfo(boolean authenticated, boolean setupRequired, String memberId,
                              String displayName, String role) {}
    public record GameDay(LocalDate date, boolean nonSchoolDay, String type, String nameHe, String nameEn) {}
    public record WeekBoard(LocalDate weekStart, LocalDate weekEnd, String mode,
                            List<GameDay> days, List<Assignment> assignments, List<MemberScore> scores) {}
    public record Bootstrap(boolean setupRequired, String familyId, String familyName,
                            ZoneId timezone, LocalDate today, boolean todayNonSchoolDay, String defaultLanguage,
                            List<FamilyMember> members, List<ChoreCategory> categories,
                            List<Chore> chores, List<Right> rights, List<MemberBalance> balances,
                            List<RightRedemption> rightRedemptions,
                            WeekBoard currentWeek, WeekBoard comingWeek) {}
}
