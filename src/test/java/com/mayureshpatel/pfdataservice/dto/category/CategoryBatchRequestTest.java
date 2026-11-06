package com.mayureshpatel.pfdataservice.dto.category;

import com.mayureshpatel.pfdataservice.dto.ValidatorTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies {@link CategoryBatchRequest}'s bean-validation limits: group count, name length and the total-name cap. */
@DisplayName("CategoryBatchRequest Validation Tests")
class CategoryBatchRequestTest extends ValidatorTestBase {

    @Test
    @DisplayName("should pass a valid request with blank subcategory lines")
    void passesValid() {
        CategoryBatchRequest request = new CategoryBatchRequest(List.of(
                new CategoryBatchGroup("Food", "EXPENSE", "#fff", "apple", List.of("Groceries", ""))));

        assertTrue(validator().validate(request).isEmpty());
    }

    @Test
    @DisplayName("should fail with more than 50 groups")
    void failsTooManyGroups() {
        List<CategoryBatchGroup> groups = IntStream.range(0, 51)
                .mapToObj(i -> new CategoryBatchGroup("G" + i, null, null, null, null)).toList();

        assertEquals(1, validator().validate(new CategoryBatchRequest(groups)).size());
    }

    @Test
    @DisplayName("should fail when the total names exceed 300")
    void failsTooManyNames() {
        List<String> subs = IntStream.range(0, 300).mapToObj(i -> "S" + i).toList();
        CategoryBatchRequest request = new CategoryBatchRequest(
                List.of(new CategoryBatchGroup("Big", null, null, null, subs)));

        assertEquals(1, validator().validate(request).size());
    }
}
