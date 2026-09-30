package org.familychoregame.domain;

public record Chore(String id, String categoryId, String nameHe, String nameEn, int points,
                    int sortOrder, boolean active) {
}
