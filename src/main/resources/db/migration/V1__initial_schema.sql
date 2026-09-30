CREATE TABLE families (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    timezone TEXT NOT NULL,
    default_language TEXT NOT NULL CHECK (default_language IN ('he', 'en')),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE family_members (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    display_name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    color TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL,
    archived_at TEXT,
    UNIQUE (family_id, normalized_name)
);

CREATE TABLE chore_categories (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    name_he TEXT NOT NULL,
    name_en TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

CREATE TABLE chores (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    category_id TEXT NOT NULL REFERENCES chore_categories(id),
    name_he TEXT NOT NULL,
    name_en TEXT NOT NULL,
    points INTEGER NOT NULL CHECK (points > 0),
    sort_order INTEGER NOT NULL,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE assignments (
    id TEXT PRIMARY KEY,
    family_id TEXT NOT NULL REFERENCES families(id),
    member_id TEXT NOT NULL REFERENCES family_members(id),
    chore_id TEXT NOT NULL REFERENCES chores(id),
    assignment_date TEXT NOT NULL,
    points_snapshot INTEGER NOT NULL CHECK (points_snapshot > 0),
    status TEXT NOT NULL CHECK (status IN ('planned', 'completed')),
    completed_at TEXT,
    request_id TEXT NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (family_id, member_id, assignment_date),
    UNIQUE (family_id, chore_id, assignment_date),
    UNIQUE (family_id, request_id)
);

CREATE INDEX assignments_family_date_idx ON assignments(family_id, assignment_date);
