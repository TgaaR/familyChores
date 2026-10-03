package org.familychoregame.service;

import static org.familychoregame.api.ApiModels.*;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.familychoregame.api.ApiException;
import org.familychoregame.config.AppProperties;
import org.familychoregame.config.CatalogSeeder;
import org.familychoregame.config.RightSeeder;
import org.familychoregame.domain.Assignment;
import org.familychoregame.domain.RightRedemption;
import org.familychoregame.domain.FamilyMember;
import org.familychoregame.repository.GameRepository;
import org.familychoregame.repository.GameRepository.NewMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.familychoregame.security.AuthenticatedMember;

@Service
public class GameService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameService.class);
    private static final Pattern VALID_NAME = Pattern.compile("^[\\p{L}\\p{M}]+(?: [\\p{L}\\p{M}]+)*$");
    private static final List<String> COLORS = List.of("#1c756d", "#e47765", "#6d72b8", "#c08b2e", "#5285a5", "#9867a5");
    private final GameRepository repository;
    private final WeekPolicy weekPolicy;
    private final AppProperties properties;
    private final CatalogSeeder catalogSeeder;
    private final RightSeeder rightSeeder;
    private final SchoolCalendar schoolCalendar;
    private final PasswordEncoder passwordEncoder;

    public GameService(GameRepository repository, WeekPolicy weekPolicy, AppProperties properties,
                       CatalogSeeder catalogSeeder, RightSeeder rightSeeder, SchoolCalendar schoolCalendar,
                       PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.weekPolicy = weekPolicy;
        this.properties = properties;
        this.catalogSeeder = catalogSeeder;
        this.rightSeeder = rightSeeder;
        this.schoolCalendar = schoolCalendar;
        this.passwordEncoder = passwordEncoder;
    }

    public Bootstrap bootstrap() {
        var family = repository.family();
        if (family.isEmpty()) {
            var today = LocalDate.now(properties.timezone());
            return new Bootstrap(true, null, null, properties.timezone(), today,
                    schoolCalendar.isNonSchoolDay(today), "he", List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), null, null);
        }
        var profile = family.get();
        var today = LocalDate.now(properties.timezone());
        return new Bootstrap(false, profile.id(), profile.name(), properties.timezone(), today,
                schoolCalendar.isNonSchoolDay(today), profile.defaultLanguage(),
                repository.allMembers(), repository.categories(), repository.chores(), repository.rights(),
                repository.balances(), repository.rightRedemptions(), board(weekPolicy.currentWeekStart()),
                board(weekPolicy.comingWeekStart()));
    }

    public Bootstrap setupFamily(SetupFamilyRequest request) {
        if (repository.family().isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "family_already_configured",
                    "This application already has a family");
        }
        var familyName = request.familyName().strip().replaceAll("\\s+", " ");
        var normalizedNames = new java.util.HashSet<String>();
        var newMembers = new java.util.ArrayList<NewMember>();
        if (request.members().stream().noneMatch(member -> member.role().equals("PARENT"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "parent_required", "At least one parent is required");
        }
        for (int index = 0; index < request.members().size(); index++) {
            var requestedMember = request.members().get(index);
            var displayName = normalizeAndValidateMemberName(requestedMember.displayName());
            var normalized = normalizedName(displayName);
            if (!normalizedNames.add(normalized)) {
                throw new ApiException(HttpStatus.CONFLICT, "duplicate_member_name",
                        "Family member names must be unique");
            }
            newMembers.add(new NewMember(UUID.randomUUID().toString(), displayName, normalized,
                    COLORS.get(index % COLORS.size()), requestedMember.role(),
                    passwordEncoder.encode(requestedMember.pin())));
        }
        repository.createFamily(familyName, properties.timezone().getId(), request.defaultLanguage(),
                List.copyOf(newMembers), Instant.now());
        catalogSeeder.synchronize();
        rightSeeder.synchronize();
        LOGGER.atInfo()
                .addKeyValue("event", "family_created")
                .addKeyValue("family_id", GameRepository.FAMILY_ID)
                .addKeyValue("member_count", newMembers.size())
                .addKeyValue("default_language", request.defaultLanguage())
                .log("Family created");
        return bootstrap();
    }

    public WeekBoard board(LocalDate weekStart) {
        if (!weekPolicy.weekStart(weekStart).equals(weekStart)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_week_start", "weekStart must be a Sunday");
        }
        var assignments = repository.assignments(weekStart, weekStart.plusDays(5));
        var scores = repository.allMembers().stream().map(member -> score(member, assignments)).toList();
        var mode = weekStart.equals(weekPolicy.currentWeekStart()) ? "execution"
                : weekStart.equals(weekPolicy.comingWeekStart()) ? "planning" : "history";
        var days = java.util.stream.IntStream.range(0, 6).mapToObj(weekStart::plusDays).map(date -> {
            var type = schoolCalendar.classify(date);
            return new GameDay(date, type.nonSchoolDay(), type.type(), type.nameHe(), type.nameEn());
        }).toList();
        return new WeekBoard(weekStart, weekStart.plusDays(5), mode, days, assignments, scores);
    }

    public FamilyMember addMember(AddMemberRequest request) {
        requireConfiguredFamily();
        var displayName = normalizeAndValidateMemberName(request.displayName());
        var normalized = normalizedName(displayName);
        var color = COLORS.get(repository.members().size() % COLORS.size());
        var created = repository.addMember(UUID.randomUUID().toString(), displayName, normalized, color,
                request.role(), passwordEncoder.encode(request.pin()), Instant.now());
        LOGGER.atInfo()
                .addKeyValue("event", "member_created")
                .addKeyValue("member_id", created.id())
                .log("Family member created");
        return created;
    }

    public void archiveMember(String id) {
        if (!repository.archiveMember(id, Instant.now())) {
            throw notFound("member_not_found", "Family member not found");
        }
        LOGGER.atInfo()
                .addKeyValue("event", "member_archived")
                .addKeyValue("member_id", id)
                .log("Family member archived");
    }

    public Assignment createAssignment(CreateAssignmentRequest request) {
        requireConfiguredFamily();
        if (!weekPolicy.canPlan(request.assignmentDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "planning_closed",
                    "Assignments can be created only for Sunday through Friday of the current or coming week");
        }
        if (!repository.activeMemberExists(request.memberId())) {
            throw notFound("member_not_found", "Active family member not found");
        }
        var chore = repository.chore(request.choreId())
                .orElseThrow(() -> notFound("chore_not_found", "Active chore not found"));
        var assignment = repository.addAssignment(UUID.randomUUID().toString(), request.requestId(), request.memberId(),
                chore, request.assignmentDate(), Instant.now());
        LOGGER.atInfo()
                .addKeyValue("event", "assignment_saved")
                .addKeyValue("assignment_id", assignment.id())
                .addKeyValue("member_id", assignment.memberId())
                .addKeyValue("chore_id", assignment.choreId())
                .addKeyValue("assignment_date", assignment.assignmentDate())
                .addKeyValue("points", assignment.pointsSnapshot())
                .log("Chore assignment saved");
        return assignment;
    }

    public void deleteAssignment(String id) {
        var assignment = findAssignment(id);
        if (!weekPolicy.canPlan(assignment.assignmentDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "planning_closed",
                    "Only current- or coming-week assignments can be deleted");
        }
        repository.deleteAssignment(id);
        LOGGER.atInfo()
                .addKeyValue("event", "assignment_deleted")
                .addKeyValue("assignment_id", assignment.id())
                .addKeyValue("member_id", assignment.memberId())
                .addKeyValue("chore_id", assignment.choreId())
                .addKeyValue("assignment_date", assignment.assignmentDate())
                .log("Chore assignment deleted");
    }

    public Assignment setCompletion(String id, CompletionRequest request) {
        var assignment = findAssignment(id);
        requireSelfOrParent(assignment.memberId());
        if (!weekPolicy.canComplete(assignment.assignmentDate())) {
            throw new ApiException(HttpStatus.CONFLICT, "completion_closed",
                    "Only current-week assignments can be completed");
        }
        if (!request.completed() && assignment.status().equals("completed")
                && repository.availablePoints(assignment.memberId()) < assignment.pointsSnapshot()) {
            throw new ApiException(HttpStatus.CONFLICT, "points_already_spent",
                    "This chore's points have already been spent on a right");
        }
        var changed = repository.setCompletion(id, request.completed(), request.version(), Instant.now());
        if (changed.isPresent()) {
            var updated = changed.get();
            LOGGER.atInfo()
                    .addKeyValue("event", request.completed() ? "assignment_completed" : "assignment_uncompleted")
                    .addKeyValue("assignment_id", updated.id())
                    .addKeyValue("member_id", updated.memberId())
                    .addKeyValue("chore_id", updated.choreId())
                    .addKeyValue("points", updated.pointsSnapshot())
                    .addKeyValue("version", updated.version())
                    .log("Chore assignment completion changed");
            return updated;
        }
        var latest = findAssignment(id);
        if (!request.completed() && latest.version() == request.version()
                && latest.status().equals("completed")
                && repository.availablePoints(latest.memberId()) < latest.pointsSnapshot()) {
            throw new ApiException(HttpStatus.CONFLICT, "points_already_spent",
                    "This chore's points have already been spent on a right");
        }
        throw new ApiException(HttpStatus.CONFLICT, "stale_version",
                "The assignment changed on another device; reload and retry");
    }

    public RightRedemption redeemRight(RedeemRightRequest request) {
        requireConfiguredFamily();
        requireSelfOrParent(request.memberId());
        var previousAttempt = repository.rightRedemptionByRequestId(request.requestId());
        if (previousAttempt.isPresent()) {
            var previous = previousAttempt.get();
            if (!previous.memberId().equals(request.memberId()) || !previous.rightId().equals(request.rightId())) {
                throw new ApiException(HttpStatus.CONFLICT, "idempotency_mismatch",
                        "The request ID was already used with different redemption data");
            }
            LOGGER.atInfo()
                    .addKeyValue("event", "right_redemption_replayed")
                    .addKeyValue("redemption_id", previous.id())
                    .addKeyValue("member_id", previous.memberId())
                    .addKeyValue("right_id", previous.rightId())
                    .log("Right redemption replayed");
            return previous;
        }
        if (!repository.activeMemberExists(request.memberId())) {
            throw notFound("member_not_found", "Active family member not found");
        }
        var right = repository.right(request.rightId())
                .orElseThrow(() -> notFound("right_not_found", "Active right not found"));
        var now = Instant.now();
        var today = now.atZone(properties.timezone()).toLocalDate();
        if (right.availabilityType().equals("WEEKEND") && !schoolCalendar.isNonSchoolDay(today)) {
            throw new ApiException(HttpStatus.CONFLICT, "right_not_available_today",
                    "This right is available only on weekends and school vacations");
        }
        if (repository.redemptionsOnDate(request.memberId(), right.id(), today, properties.timezone())
                >= right.dailyCap()) {
            throw new ApiException(HttpStatus.CONFLICT, "daily_cap_reached",
                    "This right has reached its daily redemption limit");
        }
        var redemption = repository.redeemRight(UUID.randomUUID().toString(), request.requestId(), request.memberId(),
                right, now, today, properties.timezone()).orElseThrow(() -> {
                    if (repository.redemptionsOnDate(request.memberId(), right.id(), today, properties.timezone())
                            >= right.dailyCap()) {
                        return new ApiException(HttpStatus.CONFLICT, "daily_cap_reached",
                                "This right has reached its daily redemption limit");
                    }
                    if (!repository.activeMemberExists(request.memberId())) {
                        return notFound("member_not_found", "Active family member not found");
                    }
                    return new ApiException(HttpStatus.CONFLICT, "insufficient_points",
                            "The family member does not have enough points");
                });
        if (!redemption.memberId().equals(request.memberId()) || !redemption.rightId().equals(request.rightId())) {
            throw new ApiException(HttpStatus.CONFLICT, "idempotency_mismatch",
                    "The request ID was already used with different redemption data");
        }
        LOGGER.atInfo()
                .addKeyValue("event", "right_redeemed")
                .addKeyValue("redemption_id", redemption.id())
                .addKeyValue("member_id", redemption.memberId())
                .addKeyValue("right_id", redemption.rightId())
                .addKeyValue("point_cost", redemption.pointCostSnapshot())
                .log("Right redeemed");
        return redemption;
    }

    private Assignment findAssignment(String id) {
        return repository.assignment(id).orElseThrow(() -> notFound("assignment_not_found", "Assignment not found"));
    }

    private MemberScore score(FamilyMember member, List<Assignment> assignments) {
        var memberAssignments = assignments.stream().filter(item -> item.memberId().equals(member.id())).toList();
        var completed = memberAssignments.stream().filter(item -> item.status().equals("completed")).toList();
        return new MemberScore(member.id(), completed.size(), memberAssignments.size(),
                completed.stream().mapToInt(Assignment::pointsSnapshot).sum());
    }

    private ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    private String normalizeAndValidateMemberName(String value) {
        var displayName = value.strip().replaceAll("\\s+", " ");
        if (!VALID_NAME.matcher(displayName).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_member_name",
                    "Names may contain Unicode letters and single spaces only");
        }
        return displayName;
    }

    private String normalizedName(String displayName) {
        return Normalizer.normalize(displayName, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private void requireConfiguredFamily() {
        if (repository.family().isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "family_setup_required", "Create the family first");
        }
    }

    private void requireSelfOrParent(String memberId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PARENT"))) return;
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedMember member)
                || (!member.role().equals("PARENT") && !member.id().equals(memberId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "You cannot act for this family member");
        }
    }
}
