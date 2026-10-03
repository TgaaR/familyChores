package org.familychoregame.domain;

public record Right(String id, String nameHe, String nameEn, int pointCost,
                      String availabilityType, int dailyCap, boolean active) {
}
