INSERT INTO users (username, email, password_hash, created_at, last_updated_timestamp)
VALUES(:username, :email, :passwordHash, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)