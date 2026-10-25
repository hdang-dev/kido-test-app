-- The admin list pages through users newest first (AdminUserService.list: created_at DESC, id).
CREATE INDEX ix_app_user_created_at ON app_user (created_at DESC, id);
