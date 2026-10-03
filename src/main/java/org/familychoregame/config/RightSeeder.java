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
public class RightSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RightSeeder.class);
    static final String CATALOG_PATH = "catalog/rights.txt";
    private final Jdbi jdbi;

    public RightSeeder(Jdbi jdbi) {
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
            handle.createUpdate("UPDATE rights SET active = FALSE WHERE family_id = :familyId")
                    .bind("familyId", GameRepository.FAMILY_ID).execute();
            var now = Instant.now();
            for (var right : entries) {
                handle.createUpdate("""
                        INSERT INTO rights(id, family_id, name_he, name_en, point_cost, availability_type, daily_cap,
                          active, created_at, updated_at)
                        VALUES (:id, :familyId, :nameHe, :nameEn, :cost, :availability, :dailyCap, TRUE, :now, :now)
                        ON CONFLICT(id) DO UPDATE SET name_he = excluded.name_he, name_en = excluded.name_en,
                          point_cost = excluded.point_cost, availability_type = excluded.availability_type,
                          daily_cap = excluded.daily_cap, active = TRUE, updated_at = excluded.updated_at
                        """).bind("id", right.id()).bind("familyId", GameRepository.FAMILY_ID)
                        .bind("nameHe", right.nameHe()).bind("nameEn", right.nameEn())
                        .bind("cost", right.pointCost()).bind("availability", right.availabilityType())
                        .bind("dailyCap", right.dailyCap()).bind("now", now).execute();
            }
        });
        LOGGER.atInfo()
                .addKeyValue("event", "right_catalog_synchronized")
                .addKeyValue("right_count", entries.size())
                .log("Right catalog synchronized");
    }

    private List<RightEntry> readCatalog() {
        var rights = new ArrayList<RightEntry>();
        var ids = new HashSet<String>();
        var lines = readLines();
        for (int index = 0; index < lines.size(); index++) {
            var line = lines.get(index).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var fields = line.split("\\|", -1);
            try {
                if (fields.length != 6) throw new IllegalArgumentException("expected 6 fields");
                var id = required(fields[0]);
                if (!ids.add(id)) throw new IllegalArgumentException("duplicate right id " + id);
                var availability = required(fields[4]);
                if (!availability.equals("ALL_WEEK") && !availability.equals("WEEKEND")) {
                    throw new IllegalArgumentException("availability must be ALL_WEEK or WEEKEND");
                }
                rights.add(new RightEntry(id, required(fields[1]), required(fields[2]),
                        positiveInt(fields[3]), availability, positiveInt(fields[5])));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid " + CATALOG_PATH + " line " + (index + 1)
                        + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(rights);
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

    private record RightEntry(String id, String nameHe, String nameEn, int pointCost,
                                String availabilityType, int dailyCap) {}
}
