package com.mayureshpatel.pfdataservice.repository.tag.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class TagQueries {

    // language=SQL
    public static final String FIND_BY_ID = """
            select *
            from tags
            where id = :id
            """;

    // language=SQL
    public static final String FIND_ALL_BY_USER_ID = """
            select *
            from tags
            where user_id = :userId
            order by name
            """;

    // language=SQL
    public static final String FIND_BY_ID_AND_USER_ID = """
            select *
            from tags
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    public static final String INSERT = """
            insert into tags (user_id, name, color, created_at, updated_at)
            values (:userId, :name, :color, current_timestamp, current_timestamp)
            returning id
            """;

    // language=SQL
    // PF-307: ownership-scoped, matching the three-layer pattern established for accounts,
    // merchants, and category rules -- previously unscoped (any authenticated caller who knew or
    // guessed another user's tag id could rename it).
    public static final String UPDATE = """
            update tags
            set name = :name,
                color = :color,
                updated_at = current_timestamp
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    // PF-307: ownership-scoped -- previously unscoped, the same gap UPDATE had.
    public static final String DELETE = """
            delete from tags
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    public static final String COUNT = """
            select count(*) from tags
            """;

    // language=SQL
    // PF-307: moved from TransactionQueries (which never had a real caller for it) -- this is
    // fundamentally tag-domain functionality, and TagRepository is where it's actually used.
    public static final String FIND_BY_TRANSACTION_ID = """
            select tags.*
            from transaction_tags
                join tags on transaction_tags.tag_id = tags.id
            where transaction_tags.transaction_id = :transactionId
            """;

    // language=SQL
    // PF-307: moved from TransactionQueries, same reasoning as FIND_BY_TRANSACTION_ID above.
    public static final String INSERT_TRANSACTION_TAG = """
            insert into transaction_tags (transaction_id, tag_id)
            values (:transactionId, :tagId)
            """;

    // language=SQL
    // PF-307: removes one specific tag from one specific transaction. Not the same as
    // TransactionQueries' old (equally uncalled) DELETE_TRANSACTION_TAGS, which clears every tag
    // on a transaction -- a bulk-replace operation this story doesn't need, so it wasn't carried
    // over.
    public static final String DELETE_TRANSACTION_TAG = """
            delete from transaction_tags
            where transaction_id = :transactionId
              and tag_id = :tagId
            """;
}
