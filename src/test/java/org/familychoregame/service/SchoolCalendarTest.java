package org.familychoregame.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SchoolCalendarTest {
    private final SchoolCalendar calendar = new SchoolCalendar();

    @Test
    void classifiesRegularSchoolDaysAndFridaySaturdayWeekends() {
        assertThat(calendar.classify(LocalDate.of(2026, 9, 22)).type()).isEqualTo("school-day");
        assertThat(calendar.classify(LocalDate.of(2026, 9, 18)).type()).isEqualTo("weekend");
        assertThat(calendar.classify(LocalDate.of(2026, 9, 19)).type()).isEqualTo("weekend");
    }

    @Test
    void classifiesOfficialJewishSchoolVacationRanges() {
        var sukkot = calendar.classify(LocalDate.of(2026, 9, 30));
        assertThat(sukkot.nonSchoolDay()).isTrue();
        assertThat(sukkot.type()).isEqualTo("vacation");
        assertThat(sukkot.nameEn()).isEqualTo("Sukkot vacation");
        assertThat(calendar.classify(LocalDate.of(2027, 4, 13)).type()).isEqualTo("vacation");
        assertThat(calendar.classify(LocalDate.of(2027, 4, 28)).type()).isEqualTo("vacation");
        assertThat(calendar.classify(LocalDate.of(2027, 4, 29)).type()).isEqualTo("school-day");
    }
}
