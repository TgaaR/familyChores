package org.familychoregame.domain;

import java.time.Instant;

public record FamilyMember(String id, String displayName, String color, boolean active, Instant createdAt) {
}
