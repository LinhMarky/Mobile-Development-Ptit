-- Serialize actor writes with deletion without locking the users row before room locks.
CREATE TABLE account_lifecycle_locks (
    user_id BIGINT PRIMARY KEY,
    CONSTRAINT fk_account_lifecycle_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO account_lifecycle_locks (user_id) SELECT id FROM users;
