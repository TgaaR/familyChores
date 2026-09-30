package org.familychoregame.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class WeekPolicyTest {
    private static final ZoneId JERUSALEM = ZoneId.of("Asia/Jerusalem");
    private final WeekPolicy policy = new WeekPolicy(JERUSALEM,
            Clock.fixed(Instant.parse("2026-09-24T09:00:00Z"), JERUSALEM));

    @Test
    void weekStartsOnSundayInFamilyTimezone() {
        assertThat(policy.currentWeekStart()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(policy.comingWeekStart()).isEqualTo(LocalDate.of(2026, 9, 27));
    }

    @Test
    void planningAllowsSundayThroughFridayOfCurrentAndComingWeeks() {
        assertThat(policy.canPlan(LocalDate.of(2026, 9, 20))).isTrue();
        assertThat(policy.canPlan(LocalDate.of(2026, 9, 24))).isTrue();
        assertThat(policy.canPlan(LocalDate.of(2026, 9, 26))).isFalse();
        assertThat(policy.canPlan(LocalDate.of(2026, 9, 27))).isTrue();
        assertThat(policy.canPlan(LocalDate.of(2026, 10, 2))).isTrue();
        assertThat(policy.canPlan(LocalDate.of(2026, 10, 3))).isFalse();
        assertThat(policy.canPlan(LocalDate.of(2026, 9, 19))).isFalse();
    }

    @Test
    void completionAllowsOnlySundayThroughFridayOfCurrentWeek() {
        assertThat(policy.canComplete(LocalDate.of(2026, 9, 20))).isTrue();
        assertThat(policy.canComplete(LocalDate.of(2026, 9, 25))).isTrue();
        assertThat(policy.canComplete(LocalDate.of(2026, 9, 26))).isFalse();
        assertThat(policy.canComplete(LocalDate.of(2026, 9, 27))).isFalse();
    }
}
