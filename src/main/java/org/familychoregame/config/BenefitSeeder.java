package org.familychoregame.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.familychoregame.repository.GameRepository;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class BenefitSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenefitSeeder.class);
    static final String CATALOG_PATH = "catalog/benefits.txt";
    private final Jdbi jdbi;

    public BenefitSeeder(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void run(ApplicationArguments args) {
        synchronize();
    }

    public void synchronize() {
        var entries = readCatalog();
        var familyExists = jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT EXISTS(SELECT 1 FROM families WHERE id = :familyId)")
                .bind("familyId", GameRepository.FAMILY_ID).mapTo(boolean.class).one());
        if (!familyExists) return;

        jdbi.useTransaction(handle -> {
            handle.createUpdate("UPDATE benefits SET active = 0 WHERE family_id = :familyId")
                    .bind("familyId", GameRepository.FAMILY_ID).execute();
            var now = Instant.now().toString();
            for (var benefit : entries) {
                handle.createUpdate("""
                        INSERT INTO benefits(id, family_id, name_he, name_en, point_cost, availability_type, daily_cap,
                          active, created_at, updated_at)
                        VALUES (:id, :familyId, :nameHe, :nameEn, :cost, :availability, :dailyCap, 1, :now, :now)
                        ON CONFLICT(id) DO UPDATE SET name_he = excluded.name_he, name_en = excluded.name_en,
                          point_cost = excluded.point_cost, availability_type = excluded.availability_type,
                          daily_cap = excluded.daily_cap, active = 1, updated_at = excluded.updated_at
                        """).bind("id", benefit.id()).bind("familyId", GameRepository.FAMILY_ID)
                        .bind("nameHe", benefit.nameHe()).bind("nameEn", benefit.nameEn())
                        .bind("cost", benefit.pointCost()).bind("availability", benefit.availabilityType())
                        .bind("dailyCap", benefit.dailyCap()).bind("now", now).execute();
            }
        });
        LOGGER.atInfo()
                .addKeyValue("event", "benefit_catalog_synchronized")
                .addKeyValue("benefit_count", entries.size())
                .log("Benefit catalog synchronized");
    }

    private List<BenefitEntry> readCatalog() {
        var benefits = new ArrayList<BenefitEntry>();
        var ids = new HashSet<String>();
        var lines = readLines();
        for (int index = 0; index < lines.size(); index++) {
            var line = lines.get(index).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var fields = line.split("\\|", -1);
            try {
                if (fields.length != 7) throw new IllegalArgumentException("expected 7 fields");
                if (!"BENEFIT".equals(fields[0])) throw new IllegalArgumentException("unknown record type " + fields[0]);
                var id = required(fields[1]);
                if (!ids.add(id)) throw new IllegalArgumentException("duplicate benefit id " + id);
                var availability = required(fields[5]);
                if (!availability.equals("ALL_WEEK") && !availability.equals("WEEKEND")) {
                    throw new IllegalArgumentException("availability must be ALL_WEEK or WEEKEND");
                }
                benefits.add(new BenefitEntry(id, required(fields[2]), required(fields[3]),
                        positiveInt(fields[4]), availability, positiveInt(fields[6])));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid " + CATALOG_PATH + " line " + (index + 1)
                        + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(benefits);
    }

    private List<String> readLines() {
        try (var input = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + CATALOG_PATH, exception);
        }
    }

    private static String required(String value) {
        var stripped = value.strip();
        if (stripped.isEmpty()) throw new IllegalArgumentException("fields cannot be blank");
        return stripped;
    }

    private static int positiveInt(String value) {
        var parsed = Integer.parseInt(value.strip());
        if (parsed <= 0) throw new IllegalArgumentException("numeric fields must be positive");
        return parsed;
    }

    private record BenefitEntry(String id, String nameHe, String nameEn, int pointCost,
                                String availabilityType, int dailyCap) {}
}
