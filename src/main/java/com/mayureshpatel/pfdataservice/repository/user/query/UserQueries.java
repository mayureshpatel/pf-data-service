package com.mayureshpatel.pfdataservice.repository.user.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class UserQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from users
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_EMAIL = """
            select *
            from users
            where email = :email
              and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_BY_USERNAME = """
            select *
            from users
            where username = :username
              and deleted_at is null
            """;

    // language=SQL
    public static final String FIND_ALL = """
            select *
            from users
            where deleted_at is null
            """;

    // language=SQL
    public static final String INSERT = """
            insert into users
                (
                 username,
                 email,
                 password_hash,
                 last_updated_by,
                 created_at, last_updated_timestamp)
            values (
                    :username,
                    :email,
                    :passwordHash,
                    :lastUpdatedBy,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            returning id
            """;

    // language=SQL
    public static final String UPDATE = """
            update users
            set username = :username,
                email = :email,
                password_hash = :passwordHash,
                last_updated_by = :lastUpdatedBy,
                last_updated_timestamp = CURRENT_TIMESTAMP
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String DELETE_BY_ID = """
            update users
            set deleted_at = CURRENT_TIMESTAMP
            where id = :id
            """;

    // language=SQL
    public static final String EXISTS_BY_EMAIL = """
            select count(*)
            from users
            where email = :email
              and deleted_at is null
            """;

    // language=SQL
    public static final String EXISTS_BY_USERNAME = """
            select count(*)
            from users
            where username = :username
              and deleted_at is null
            """;

    // language=SQL
    public static final String EXISTS_BY_ID = """
            select count(*)
            from users
            where id = :id
              and deleted_at is null
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*)
            from users
            where deleted_at is null
            """;
}
