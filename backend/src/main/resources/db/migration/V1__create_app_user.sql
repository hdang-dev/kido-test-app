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

    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN'))
);

-- Case-insensitive uniqueness enforced by the DB itself. The app also stores emails normalised
-- (Emails.normalise), but uniqueness doesn't depend on Java and Postgres agreeing on lower-casing.
CREATE UNIQUE INDEX uq_app_user_email_ci ON app_user (lower(email));

-- At most one SUPER_ADMIN, ever (design.md §5).
CREATE UNIQUE INDEX uq_app_user_single_super_admin ON app_user (role) WHERE role = 'SUPER_ADMIN';
