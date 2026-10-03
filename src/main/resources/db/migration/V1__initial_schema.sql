CREATE TABLE families (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    timezone TEXT NOT NULL,
    default_language TEXT NOT NULL CHECK (default_language IN ('he', 'en')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE family_members (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    display_name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    color TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('PARENT', 'CHILD')),
    pin_hash TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ,
    UNIQUE (family_id, normalized_name)
);

CREATE TABLE member_login_attempts (
    member_id TEXT PRIMARY KEY REFERENCES family_members(id) ON DELETE CASCADE,
    failed_attempts INTEGER NOT NULL CHECK (failed_attempts >= 0),
    locked_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE chore_categories (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    name_he TEXT NOT NULL,
    name_en TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE chores (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    category_id TEXT NOT NULL REFERENCES chore_categories(id),
    name_he TEXT NOT NULL,
    name_en TEXT NOT NULL,
    points INTEGER NOT NULL CHECK (points > 0),
    sort_order INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE assignments (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    member_id TEXT NOT NULL REFERENCES family_members(id),
    chore_id TEXT NOT NULL REFERENCES chores(id),
    assignment_date DATE NOT NULL,
    points_snapshot INTEGER NOT NULL CHECK (points_snapshot > 0),
    status TEXT NOT NULL CHECK (status IN ('planned', 'completed')),
    completed_at TIMESTAMPTZ,
    request_id TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (family_id, member_id, assignment_date),
    UNIQUE (family_id, chore_id, assignment_date),
    UNIQUE (family_id, request_id)
);

CREATE INDEX assignments_family_date_idx ON assignments(family_id, assignment_date);

CREATE TABLE rights (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    name_he TEXT NOT NULL,
    name_en TEXT NOT NULL,
    point_cost INTEGER NOT NULL CHECK (point_cost > 0),
    availability_type TEXT NOT NULL DEFAULT 'ALL_WEEK'
        CHECK (availability_type IN ('ALL_WEEK', 'WEEKEND')),
    daily_cap INTEGER NOT NULL DEFAULT 1 CHECK (daily_cap > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE right_redemptions (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    member_id TEXT NOT NULL REFERENCES family_members(id),
    right_id TEXT NOT NULL REFERENCES rights(id),
    right_name_he_snapshot TEXT NOT NULL,
    right_name_en_snapshot TEXT NOT NULL,
    point_cost_snapshot INTEGER NOT NULL CHECK (point_cost_snapshot > 0),
    request_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (family_id, request_id)
);

CREATE INDEX right_redemptions_family_member_idx
    ON right_redemptions(family_id, member_id, created_at);

CREATE INDEX right_redemptions_daily_cap_idx
    ON right_redemptions(family_id, member_id, right_id, created_at);
