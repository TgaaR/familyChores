CREATE TABLE benefits (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    name_he TEXT NOT NULL,
    name_en TEXT NOT NULL,
    point_cost INTEGER NOT NULL CHECK (point_cost > 0),
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE benefit_redemptions (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    member_id TEXT NOT NULL REFERENCES family_members(id),
    benefit_id TEXT NOT NULL REFERENCES benefits(id),
    benefit_name_he_snapshot TEXT NOT NULL,
    benefit_name_en_snapshot TEXT NOT NULL,
    point_cost_snapshot INTEGER NOT NULL CHECK (point_cost_snapshot > 0),
    request_id TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE (family_id, request_id)
);

CREATE INDEX benefit_redemptions_family_member_idx
    ON benefit_redemptions(family_id, member_id, created_at);
