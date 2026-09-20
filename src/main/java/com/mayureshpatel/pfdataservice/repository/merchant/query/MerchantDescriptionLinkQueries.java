package com.mayureshpatel.pfdataservice.repository.merchant.query;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class MerchantDescriptionLinkQueries {

    // language=SQL
    public static final String FIND_MERCHANT_IDS_BY_NORMALIZED_DESCRIPTIONS = """
            select normalized_description, merchant_id
            from merchant_description_links
            where user_id = :userId
              and normalized_description in (:normalizedDescriptions)
            """;

    // language=SQL -- one method/query serves both the auto-capture path and the explicit
    // add-link endpoint; last-write-wins on the (user_id, normalized_description) unique index.
    public static final String UPSERT = """
            insert into merchant_description_links (user_id, merchant_id, description, normalized_description, created_at, updated_at)
            values (:userId, :merchantId, :description, :normalizedDescription, now(), now())
            on conflict (user_id, normalized_description) do update
                set merchant_id = excluded.merchant_id,
                    description = excluded.description,
                    updated_at = now()
            """;

    // language=SQL
    public static final String FIND_BY_ID_AND_USER_ID = """
            select *
            from merchant_description_links
            where id = :id
              and user_id = :userId
            """;

    // language=SQL
    public static final String FIND_BY_MERCHANT_ID_AND_USER_ID = """
            select *
            from merchant_description_links
            where merchant_id = :merchantId
              and user_id = :userId
            order by id
            """;

    // language=SQL
    public static final String DELETE_BY_ID_AND_USER_ID = """
            delete from merchant_description_links
            where id = :id
              and user_id = :userId
            """;
}
