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
@Order(10)
public class CatalogSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogSeeder.class);
    static final String CATALOG_PATH = "catalog/choresAndScores.txt";
    private final Jdbi jdbi;

    public CatalogSeeder(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void run(ApplicationArguments args) {
        var catalog = readCatalog();
        synchronize(catalog);
    }

    public void synchronize() {
        synchronize(readCatalog());
    }

    private void synchronize(Catalog catalog) {
        var familyExists = jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT EXISTS(SELECT 1 FROM families WHERE id = :familyId)")
                .bind("familyId", GameRepository.FAMILY_ID).mapTo(boolean.class).one());
        if (!familyExists) return;
        jdbi.useTransaction(handle -> {
            handle.createUpdate("UPDATE chores SET active = 0 WHERE family_id = :familyId")
                    .bind("familyId", GameRepository.FAMILY_ID).execute();
            handle.createUpdate("UPDATE chore_categories SET active = 0 WHERE family_id = :familyId")
                    .bind("familyId", GameRepository.FAMILY_ID).execute();

            for (var category : catalog.categories()) {
                handle.createUpdate("""
                        INSERT INTO chore_categories(id, family_id, name_he, name_en, sort_order, active)
                        VALUES (:id, :familyId, :nameHe, :nameEn, 0, 1)
                        ON CONFLICT(id) DO UPDATE SET name_he = excluded.name_he, name_en = excluded.name_en,
                          sort_order = 0, active = 1
                        """).bind("id", category.id()).bind("familyId", GameRepository.FAMILY_ID)
                        .bind("nameHe", category.nameHe()).bind("nameEn", category.nameEn()).execute();
            }

            var now = Instant.now().toString();
            for (var chore : catalog.chores()) {
                handle.createUpdate("""
                        INSERT INTO chores(id, family_id, category_id, name_he, name_en, points, sort_order,
                          active, created_at, updated_at)
                        VALUES (:id, :familyId, :categoryId, :nameHe, :nameEn, :points, 0, 1, :now, :now)
                        ON CONFLICT(id) DO UPDATE SET category_id = excluded.category_id,
                          name_he = excluded.name_he, name_en = excluded.name_en, points = excluded.points,
                          sort_order = 0, active = 1, updated_at = excluded.updated_at
                        """).bind("id", chore.id()).bind("familyId", GameRepository.FAMILY_ID)
                        .bind("categoryId", chore.categoryId()).bind("nameHe", chore.nameHe())
                        .bind("nameEn", chore.nameEn()).bind("points", chore.points())
                        .bind("now", now).execute();
            }
        });
        LOGGER.atInfo()
                .addKeyValue("event", "chore_catalog_synchronized")
                .addKeyValue("category_count", catalog.categories().size())
                .addKeyValue("chore_count", catalog.chores().size())
                .log("Chore catalog synchronized");
    }

    private Catalog readCatalog() {
        var categories = new ArrayList<CategoryEntry>();
        var chores = new ArrayList<ChoreEntry>();
        var categoryIds = new HashSet<String>();
        var choreIds = new HashSet<String>();
        var lines = readLines();
        for (int index = 0; index < lines.size(); index++) {
            var line = lines.get(index).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var fields = line.split("\\|", -1);
            try {
                switch (fields[0]) {
                    case "CATEGORY" -> {
                        requireFieldCount(fields, 4);
                        var id = required(fields[1]);
                        if (!categoryIds.add(id)) throw new IllegalArgumentException("duplicate category id " + id);
                        categories.add(new CategoryEntry(id, required(fields[2]), required(fields[3])));
                    }
                    case "CHORE" -> {
                        requireFieldCount(fields, 6);
                        var id = required(fields[1]);
                        if (!choreIds.add(id)) throw new IllegalArgumentException("duplicate chore id " + id);
                        chores.add(new ChoreEntry(id, required(fields[2]), required(fields[3]),
                                required(fields[4]), positiveInt(fields[5])));
                    }
                    default -> throw new IllegalArgumentException("unknown record type " + fields[0]);
                }
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid " + CATALOG_PATH + " line " + (index + 1)
                        + ": " + exception.getMessage(), exception);
            }
        }
        if (categories.isEmpty() || chores.isEmpty()) {
            throw new IllegalStateException(CATALOG_PATH + " must contain at least one category and chore");
        }
        for (var chore : chores) {
            if (!categoryIds.contains(chore.categoryId())) {
                throw new IllegalStateException(CATALOG_PATH + " chore " + chore.id()
                        + " references missing category " + chore.categoryId());
            }
        }
        return new Catalog(List.copyOf(categories), List.copyOf(chores));
    }

    private List<String> readLines() {
        try (var input = new ClassPathResource(CATALOG_PATH).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + CATALOG_PATH, exception);
        }
    }

    private static void requireFieldCount(String[] fields, int expected) {
        if (fields.length != expected) throw new IllegalArgumentException("expected " + expected + " fields");
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

    private record Catalog(List<CategoryEntry> categories, List<ChoreEntry> chores) {}
    private record CategoryEntry(String id, String nameHe, String nameEn) {}
    private record ChoreEntry(String id, String categoryId, String nameHe, String nameEn,
                              int points) {}
}
