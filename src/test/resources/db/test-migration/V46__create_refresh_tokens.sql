-- V46: Refresh tokens (PF-899)
-- Purpose: server-side record of each signed-in session's refresh token so an active user can be
--          kept signed in (short-lived access token, renewed through the HttpOnly refresh cookie)
--          and so signing out really ends the session. Only the SHA-256 hash of the random token
--          is stored; the raw token lives only in the user's cookie. Tokens are not rotated: the
--          idle expiry slides on use, the absolute expiry never moves. Deleting the owning user
--          deletes their tokens. Storage only -- the endpoints and cookie come later.

CREATE TABLE refresh_tokens
(
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash          VARCHAR(64) NOT NULL UNIQUE,
    remember_me         BOOLEAN     NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    idle_expires_at     TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,
    revoked_at          TIMESTAMPTZ
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_idle_expires_at ON refresh_tokens (idle_expires_at);
CREATE INDEX idx_refresh_tokens_absolute_expires_at ON refresh_tokens (absolute_expires_at);

COMMENT ON TABLE refresh_tokens IS
    'Server-side record of a session refresh token (hash only); lets an active user stay signed in and makes sign-out revoke the session.';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'Hex SHA-256 of the random 256-bit token; the raw token is never stored.';
COMMENT ON COLUMN refresh_tokens.remember_me IS 'True for a remember-me session (long idle and absolute windows), false for a browser-session one.';
COMMENT ON COLUMN refresh_tokens.idle_expires_at IS 'Slides forward on each use, never beyond absolute_expires_at.';
COMMENT ON COLUMN refresh_tokens.absolute_expires_at IS 'Hard end of the session regardless of activity.';
COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Set on sign-out; a revoked token is never accepted again.';
