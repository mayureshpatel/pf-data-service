package com.mayureshpatel.pfdataservice.repository;

import com.mayureshpatel.pfdataservice.service.DescriptionKeys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the description key is folded identically in Java ({@code DescriptionKeys.fold}) and in
 * PostgreSQL ({@code DescriptionKeys.sqlFold}): a merchant link, a recurring pattern and a dismissal
 * match on this key, so a description the two disagree about would link in one place and not another.
 */
@DisplayName("Description key Java/PostgreSQL parity")
class DescriptionKeyParityTest extends BaseRepositoryTest {

    private static final List<String> DESCRIPTIONS = List.of(
            "  Netflix  ",
            "NETFLIX.COM   BILL",
            "Net flix",
            "Net flix Inc",
            "Net\tflix\nInc\r",
            "  Netflix　",
            "CAFÉ  MÜNCHEN",
            "already folded");

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @DisplayName("should fold every description to the same key in Java and in PostgreSQL")
    void shouldFoldEveryDescriptionToTheSameKeyInJavaAndInPostgreSql() {
        for (String description : DESCRIPTIONS) {
            // act
            String sql = jdbcClient.sql("select " + DescriptionKeys.sqlFold(":description"))
                    .param("description", description)
                    .query(String.class)
                    .single();

            // assert & verify
            assertEquals(DescriptionKeys.fold(description), sql, "description: " + description.replace(" ", "<nbsp>"));
        }
    }
}
