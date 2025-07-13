INSERT INTO app_user (email, password_hash, full_name)
VALUES ('admin@example.com', digest('ChangeMe123', 'sha-256')::bytea, 'Admin User');