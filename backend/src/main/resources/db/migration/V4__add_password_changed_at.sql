-- Credential version for sessions (design.md D10). Each session records the value it was created with;
-- SessionValidityFilter signs out any session older than the user's last password change.
ALTER TABLE app_user ADD COLUMN password_changed_at timestamptz NOT NULL DEFAULT now();
