package org.familychoregame.domain;

import java.time.Instant;

public record RightRedemption(String id, String memberId, String rightId,
                                String rightNameHeSnapshot, String rightNameEnSnapshot,
                                int pointCostSnapshot, Instant createdAt) {
}
