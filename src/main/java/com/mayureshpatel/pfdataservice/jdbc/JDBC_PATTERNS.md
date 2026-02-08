# JDBC Client Patterns & Best Practices

## Basic Query Patterns

---

### Simple Select

```java
List<Entity> findAll() {
    return jdbcClient.sql("SELECT * FROM table WHERE deleted_at IS NULL")
            .query(entityRowMapper)
            .list();
}
```

### Parameterized Query

```java
Optional<Entity> findById(Long id) {
    return jdbcClient.sql(
                """
                    SELECT * FROM table
                    WHERE id = :id
                        AND deleted_at IS NULL
                """
            )
            .param("id", id)
            .query(entityRowMapper)
            .optional();
}
```

### Insert with Generated Key

```java
Entity save(Entity entity) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    
    jdbcClient.sql("""
            INSERT INTO table (name, value, created_at)
            VALUES (:name, :value, CURRENT_TIMESTAMP
            """)
            .param("name", entity.getName())
            .param("value", entity.getValue())
            .update(keyHolder);
    
    entity.setId(keyHolder.getKeyAs(Long.class));
    return entity;
}
```

### Update

```java
void update(Entity entity) {
    jdbcClient.sql("""
            UPDATE table
            SET deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :userId
            WHERE id = :id
            """)
            .param("name", entity.getName())
            .param("value", entity.getValue())
            .param("id", entity.getId())
            .update();
}
```

### Soft Delete
```java
void deleteById(Long id) {
    jdbcClient.sql("""
            UPDATE table
            SET deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :userId
            WHERE id = :id
            """)
            .param("id", id)
            .param("userId", getCurrentUserId())
            .update();
}
```

## Handling Relationships

---

### One-to-Many (Fetch Separately)
```java
Account findByIdWithTransactions(Long id) {
    Account account = jdbcClient.sql("SELECT * FROM account WHERE id = :id")
            .param("id", id)
            .query(accountRowMapper)
            .single();
    
    List<Transaction> transactions = jdbcClient.sql("""
            SELECT * FROM transactions
            WHERE account_id = :accountId
                AND deleted_at IS NULL
            """)
            .param("accountId", id)
            .query(transactionRowMapper)
            .list();
    
    account.setTransactions(transactions);
    return account;
}
```

### Many-to-One (JOIN)
```java
Transaction findByIdWithAccount(Long id) {
    return jdbcClient.sql("""
        SELECT
            t.*,
            a.id as account_id,
            a.name as account_name,
            a.current_balance as account_balance
        FROM transactions t
        JOIN accounts a ON t.account_id = a.id
        WHERE t.id = :id
          AND t.deleted_at IS NULL
        """)
        .param("id", id)
        .query((rs, rowNum) -> {
            Transaction t = transactionRowMapper.mapRow(rs, rowNum);
            Account a = new Account();
            a.setId(rs.getLong("account_id"));
            a.setName(rs.getString("account_name"));
            a.setCurrentBalance(rs.getBigDecimal("account_balance"));
            t.setAccount(a);
            return t;
        })
        .single();
}
```

### Many-to-Many (Two Queries)
```java
Transaction findByIdWithTags(Long id) {
    Transaction transaction = findById(id).orElseThrow();

    List<Tag> tags = jdbcClient.sql("""
        SELECT t.*
        FROM tags t
        JOIN transaction_tags tt ON t.id = tt.tag_id
        WHERE tt.transaction_id = :transactionId
        """)
        .param("transactionId", id)
        .query(tagRowMapper)
        .list();

    transaction.setTags(tags);
    return transaction;
}
```

## Complex Queries

---

### Aggregation
```java
BigDecimal sumBalanceByUserId(Long userId) {
    return jdbcClient.sql("""
        SELECT COALESCE(SUM(current_balance), 0)
        FROM accounts
        WHERE user_id = :userId
          AND deleted_at IS NULL
        """)
        .param("userId", userId)
        .query(BigDecimal.class)
        .single();
}
```

### DTO Projection
```java
List<CategoryTotal> findCategoryTotals(Long userId, LocalDate start, LocalDate end) {
    return jdbcClient.sql("""
        SELECT
            COALESCE(c.name, 'Uncategorized') as category_name,
            SUM(t.amount) as total
        FROM transactions t
        LEFT JOIN categories c ON t.category_id = c.id
        WHERE t.account_id IN (
            SELECT id FROM accounts WHERE user_id = :userId
        )
          AND t.date BETWEEN :start AND :end
          AND t.type = 'EXPENSE'
          AND t.deleted_at IS NULL
        GROUP BY c.name
        ORDER BY total DESC
        """)
        .param("userId", userId)
        .param("start", start)
        .param("end", end)
        .query((rs, rowNum) -> new CategoryTotal(
            rs.getString("category_name"),
            rs.getBigDecimal("total")
        ))
        .list();
}
```

### Dynamic WHERE Clause
```java
List<Transaction> findByFilter(TransactionFilter filter) {
    StringBuilder sql = new StringBuilder("""
        SELECT * FROM transactions t
        WHERE t.account_id IN (
            SELECT id FROM accounts WHERE user_id = :userId
        )
          AND t.deleted_at IS NULL
        """);

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("userId", filter.userId());

    if (filter.type() != null) {
        sql.append(" AND t.type = :type");
        params.addValue("type", filter.type().name());
    }

    if (filter.minAmount() != null) {
        sql.append(" AND t.amount >= :minAmount");
        params.addValue("minAmount", filter.minAmount());
    }

    // ... more conditions

    return jdbcClient.sql(sql.toString())
        .params(params)
        .query(transactionRowMapper)
        .list();
}
```
