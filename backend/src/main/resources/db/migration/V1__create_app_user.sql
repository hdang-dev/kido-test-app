-- Users of the app (design.md §4). Email is the login identity; username is a non-unique display name (D5).
CREATE TABLE app_user (
    id            uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    username      varchar(50)  NOT NULL,
    bio           varchar(500),
    role          varchar(20)  NOT NULL DEFAULT 'USER',
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),

    CONSTRAINT uq_app_user_email UNIQUE (email),
    -- The app normalises emails; the DB guarantees it so uniqueness is effectively case-insensitive.
    CONSTRAINT ck_app_user_email_normalised CHECK (email = lower(btrim(email))),
    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN'))
);

-- At most one SUPER_ADMIN, ever (design.md §5).
CREATE UNIQUE INDEX uq_app_user_single_super_admin ON app_user (role) WHERE role = 'SUPER_ADMIN';
