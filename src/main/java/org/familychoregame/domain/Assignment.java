package org.familychoregame.domain;

import java.time.Instant;
import java.time.LocalDate;

public record Assignment(String id, String memberId, String choreId, LocalDate assignmentDate,
                         int pointsSnapshot, String status, Instant completedAt, long version,
                         Instant createdAt, Instant updatedAt) {
}
