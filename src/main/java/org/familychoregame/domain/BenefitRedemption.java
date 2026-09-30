package org.familychoregame.domain;

import java.time.Instant;

public record BenefitRedemption(String id, String memberId, String benefitId,
                                String benefitNameHeSnapshot, String benefitNameEnSnapshot,
                                int pointCostSnapshot, Instant createdAt) {
}
