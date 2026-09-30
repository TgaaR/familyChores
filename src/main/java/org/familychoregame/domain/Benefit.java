package org.familychoregame.domain;

public record Benefit(String id, String nameHe, String nameEn, int pointCost,
                      String availabilityType, int dailyCap, boolean active) {
}
