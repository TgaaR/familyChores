package org.familychoregame.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import org.familychoregame.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WeekPolicy {
    private final ZoneId timezone;
    private final Clock clock;

    @Autowired
    public WeekPolicy(AppProperties properties) {
        this(properties.timezone(), Clock.system(properties.timezone()));
    }

    WeekPolicy(ZoneId timezone, Clock clock) {
        this.timezone = timezone;
        this.clock = clock;
    }

    public LocalDate currentWeekStart() {
        return weekStart(LocalDate.now(clock));
    }

    public LocalDate comingWeekStart() {
        return currentWeekStart().plusWeeks(1);
    }

    public LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    }

    public boolean isChoreDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY;
    }

    public boolean canPlan(LocalDate date) {
        var weekStart = weekStart(date);
        return isChoreDay(date)
                && (weekStart.equals(currentWeekStart()) || weekStart.equals(comingWeekStart()));
    }

    public boolean canComplete(LocalDate date) {
        return isChoreDay(date) && weekStart(date).equals(currentWeekStart());
    }

    public ZoneId timezone() {
        return timezone;
    }
}
