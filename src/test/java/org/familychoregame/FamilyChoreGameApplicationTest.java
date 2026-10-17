package org.familychoregame;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.familychoregame.repository.GameRepository;
import org.familychoregame.config.CatalogSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.core.io.ClassPathResource;

@SpringBootTest
class FamilyChoreGameApplicationTest {
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        PostgresTestDatabase.configure(registry, "application");
    }

    @Autowired
    GameRepository repository;
    @Autowired
    CatalogSeeder catalogSeeder;

    @BeforeEach
    void configureFamily() {
        if (repository.family().isEmpty()) {
            repository.createFamily("Test Family", "Asia/Jerusalem", "he", java.util.List.of(), Instant.now());
            catalogSeeder.synchronize();
        }
    }

    @Test
    void startupLoadsExpectedCatalogFromResourceFile() {
        assertThat(repository.categories()).hasSize(10);
        assertThat(repository.chores()).hasSize(35);
        assertThat(repository.chore("bathroom-counter")).get()
                .extracting("nameHe", "nameEn", "points")
                .containsExactly("לסדר ולנקות את השיש", "Tidy and clean the bathroom counter", 2);
        assertThat(repository.categories()).extracting("nameEn")
                .containsExactly("All over the house", "Bathroom", "Children's rooms", "Dining and play areas", "Home entrance",
                        "Kids related", "Kitchen", "Living room", "Out chore", "Toilets and laundry room");
        assertThat(repository.chores()).extracting(chore -> chore.nameEn())
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
        assertThat(repository.chores().stream()
                .filter(chore -> chore.categoryId().equals("kitchen"))
                .map(chore -> chore.nameHe() + ":" + chore.points())
                .toList()).containsExactlyInAnyOrder(
                "להעמיס מדיח:2",
                "לרוקן מדיח:2",
                "לפנות כיור:3",
                "לנקות ולסדר שיש:2",
                "להשתתף בהכנת ארוחה משפחתית:2",
                "להעמיס ולרוקן מדיח:3",
                "לנקות כריים:1",
                "להכין לחם:1",
                "להכין סלט לארוחה משפחתית:2");
        assertThat(repository.chores().stream()
                .filter(chore -> chore.categoryId().equals("toilets-laundry-room"))
                .map(chore -> chore.nameHe() + ":" + chore.points())
                .toList()).containsExactlyInAnyOrder(
                "לנקות אסלות:3",
                "לתלות כביסה:2",
                "להוריד כביסה מהחבל ולקפל:1",
                "לפזר כביסה בארונות:1");
        assertThat(repository.chores().stream()
                .filter(chore -> chore.categoryId().equals("dining-play"))
                .map(chore -> chore.nameHe() + ":" + chore.points())
                .toList()).contains(
                "לפנות דברים משותפים מהשולחן:1",
                "לערוך שולחן לארוחה משפחתית:1");
        assertThat(repository.chores().stream()
                .filter(chore -> chore.categoryId().equals("home-entrance"))
                .map(chore -> chore.nameHe() + ":" + chore.points())
                .toList()).containsExactly("לסדר שטיח ושידה:1");
        assertThat(repository.chores().stream()
                .filter(chore -> chore.categoryId().equals("children-rooms"))
                .map(chore -> chore.nameHe() + ":" + chore.points())
                .toList()).containsExactlyInAnyOrder(
                "לסדר ריצפה ושטיחים:1",
                "החלפת מצעים:1");
    }

    @Test
    void assignmentsPersistPointSnapshotsAndMapBackToDomainRecords() {
        var suffix = UUID.randomUUID().toString();
        var member = repository.addMember(suffix, "Test Member", "test member " + suffix,
                "#1c756d", Instant.now());
        var chore = repository.chore("bathroom-counter").orElseThrow();

        var assignment = repository.addAssignment(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                member.id(), chore, LocalDate.of(2030, 1, 4), Instant.now());

        assertThat(repository.assignment(assignment.id())).get()
                .extracting("pointsSnapshot", "status", "version")
                .containsExactly(2, "planned", 0L);
    }

    @Test
    void awsProfileUsesStructuredConsoleLogging() throws Exception {
        assertThat(classpathText("application-aws.yml"))
                .contains("console: ecs")
                .contains("org.jdbi: WARN");
        assertThat(classpathText("application.yml"))
                .contains("%X{request_id:-}")
                .contains("%kvp");
    }

    private String classpathText(String path) throws Exception {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
