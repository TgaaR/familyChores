ALTER TABLE benefits ADD COLUMN availability_type TEXT NOT NULL DEFAULT 'ALL_WEEK'
    CHECK (availability_type IN ('ALL_WEEK', 'WEEKEND'));

ALTER TABLE benefits ADD COLUMN daily_cap INTEGER NOT NULL DEFAULT 1
    CHECK (daily_cap > 0);

CREATE INDEX benefit_redemptions_daily_cap_idx
    ON benefit_redemptions(family_id, member_id, benefit_id, created_at);
