package org.familychoregame.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class SchoolCalendar {
    static final String CALENDAR_PATH = "calendar/jewish-school-vacations-2026-2027.txt";
    private final List<Vacation> vacations;

    public SchoolCalendar() {
        this.vacations = readCalendar();
    }

    public DayType classify(LocalDate date) {
        var vacation = vacation(date);
        if (vacation.isPresent()) {
            return new DayType(true, "vacation", vacation.get().nameHe(), vacation.get().nameEn());
        }
        if (isWeekend(date)) {
            return new DayType(true, "weekend", "סוף שבוע", "Weekend");
        }
        return new DayType(false, "school-day", "יום לימודים", "School day");
    }

    public boolean isNonSchoolDay(LocalDate date) {
        return classify(date).nonSchoolDay();
    }

    private Optional<Vacation> vacation(LocalDate date) {
        return vacations.stream().filter(item -> !date.isBefore(item.from()) && !date.isAfter(item.through()))
                .findFirst();
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.FRIDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY;
    }

    private List<Vacation> readCalendar() {
        var entries = new ArrayList<Vacation>();
        var ids = new HashSet<String>();
        var lines = readLines();
        for (int index = 0; index < lines.size(); index++) {
            var line = lines.get(index).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            var fields = line.split("\\|", -1);
            try {
                if (fields.length != 6) throw new IllegalArgumentException("expected 6 fields");
                if (!fields[0].equals("VACATION")) throw new IllegalArgumentException("unknown record type " + fields[0]);
                var id = required(fields[1]);
                if (!ids.add(id)) throw new IllegalArgumentException("duplicate vacation id " + id);
                var from = LocalDate.parse(required(fields[4]));
                var through = LocalDate.parse(required(fields[5]));
                if (through.isBefore(from)) throw new IllegalArgumentException("last date is before first date");
                entries.add(new Vacation(id, required(fields[2]), required(fields[3]), from, through));
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Invalid " + CALENDAR_PATH + " line " + (index + 1)
                        + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(entries);
    }

    private List<String> readLines() {
        try (var input = new ClassPathResource(CALENDAR_PATH).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + CALENDAR_PATH, exception);
        }
    }

    private static String required(String value) {
        var stripped = value.strip();
        if (stripped.isEmpty()) throw new IllegalArgumentException("fields cannot be blank");
        return stripped;
    }

    public record DayType(boolean nonSchoolDay, String type, String nameHe, String nameEn) {}
    private record Vacation(String id, String nameHe, String nameEn, LocalDate from, LocalDate through) {}
}
