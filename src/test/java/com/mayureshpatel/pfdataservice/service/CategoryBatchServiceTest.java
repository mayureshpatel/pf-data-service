package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchGroup;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchResultDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies {@link CategoryBatchService}: creation with inherited properties, skipping of existing and duplicate names, and the input limits. */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryBatchService Unit Tests")
class CategoryBatchServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EXPENSE = "EXPENSE";
    private static final String FOOD = "Food";
    private static final String GROCERIES = "Groceries";

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryBatchService service;

    private long nextId;

    @BeforeEach
    void setUp() {
        nextId = 100;
    }

    private void stubUserAndInserts(List<Category> existing) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).build()));
        when(categoryRepository.findByUserId(USER_ID)).thenReturn(existing);
        when(categoryRepository.insert(any(CategoryCreateRequest.class))).thenAnswer(i -> (int) nextId++);
    }

    private static CategoryBatchRequest request(CategoryBatchGroup... groups) {
        return new CategoryBatchRequest(List.of(groups));
    }

    private static CategoryBatchGroup group(String name, String... subs) {
        return new CategoryBatchGroup(name, EXPENSE, "#112233", "home", List.of(subs));
    }

    private List<CategoryCreateRequest> insertedRequests(int times) {
        ArgumentCaptor<CategoryCreateRequest> captor = ArgumentCaptor.forClass(CategoryCreateRequest.class);
        verify(categoryRepository, times(times)).insert(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName("should create a parent and subcategories that inherit type, color and icon")
    void createsParentAndSubsWithInheritedProperties() {
        // arrange
        stubUserAndInserts(List.of());

        // act
        CategoryBatchResultDto result = service.createBatch(USER_ID, request(group("Housing", "Rent", "Repairs")));

        // assert & verify
        assertEquals(3, result.createdCount());
        assertEquals(1, result.createdParents());
        assertEquals(2, result.createdSubcategories());
        assertTrue(result.skipped().isEmpty());
        List<CategoryCreateRequest> inserted = insertedRequests(3);
        assertEquals("Housing", inserted.get(0).getName());
        assertEquals(null, inserted.get(0).getParentId());
        for (CategoryCreateRequest sub : inserted.subList(1, 3)) {
            assertEquals(100L, sub.getParentId());
            assertEquals(EXPENSE, sub.getType());
            assertEquals("#112233", sub.getColor());
            assertEquals("home", sub.getIcon());
            assertEquals(USER_ID, sub.getUserId());
        }
    }

    @Test
    @DisplayName("should create several parents")
    void createsMultipleParents() {
        // arrange
        stubUserAndInserts(List.of());

        // act
        CategoryBatchResultDto result = service.createBatch(USER_ID,
                request(group("Housing", "Rent"), group(FOOD, GROCERIES)));

        // assert & verify
        assertEquals(4, result.createdCount());
        assertEquals(2, result.createdParents());
        verify(categoryRepository, times(4)).insert(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("should ignore blank subcategory lines")
    void ignoresBlankLines() {
        // arrange
        stubUserAndInserts(List.of());

        // act
        CategoryBatchResultDto result = service.createBatch(USER_ID, request(group(FOOD, "", "  ", GROCERIES)));

        // assert & verify
        assertEquals(2, result.createdCount());
        assertTrue(result.skipped().isEmpty());
    }

    @Test
    @DisplayName("should reuse an existing parent, add new subs under it and report the parent skipped")
    void reusesExistingParent() {
        // arrange
        Category existing = Category.builder().id(7L).userId(USER_ID).name("food").type("INCOME")
                .color("#000000").icon("apple").build();
        stubUserAndInserts(List.of(existing));

        // act
        CategoryBatchResultDto result = service.createBatch(USER_ID, request(group(" Food ", GROCERIES)));

        // assert & verify
        assertEquals(1, result.createdCount());
        assertEquals(0, result.createdParents());
        assertEquals(1, result.skipped().size());
        assertEquals(FOOD, result.skipped().get(0).name());
        assertEquals(null, result.skipped().get(0).parentName());
        assertEquals("Already exists", result.skipped().get(0).reason());
        CategoryCreateRequest sub = insertedRequests(1).get(0);
        assertEquals(7L, sub.getParentId());
        assertEquals("INCOME", sub.getType());
        assertEquals("#000000", sub.getColor());
        assertEquals("apple", sub.getIcon());
    }

    @Test
    @DisplayName("should skip and report a subcategory that already exists under the parent")
    void skipsExistingSub() {
        // arrange
        Category parent = Category.builder().id(7L).userId(USER_ID).name(FOOD).type(EXPENSE).build();
        Category sub = Category.builder().id(8L).userId(USER_ID).name("groceries").parentId(7L).build();
        stubUserAndInserts(List.of(parent, sub));

        // act
        CategoryBatchResultDto result = service.createBatch(USER_ID, request(group(FOOD, GROCERIES, "Dining")));

        // assert & verify
        assertEquals(1, result.createdSubcategories());
        assertEquals(2, result.skipped().size());
        assertEquals(GROCERIES, result.skipped().get(1).name());
        assertEquals(FOOD, result.skipped().get(1).parentName());
    }

    @Test
    @DisplayName("should not treat the same name under another parent as a duplicate")
    void sameSubNameUnderOtherParentIsCreated() {
        // arrange
        Category other = Category.builder().id(9L).userId(USER_ID).name("Other").build();
        Category sub = Category.builder().id(10L).userId(USER_ID).name("Misc").parentId(9L).build();
        stubUserAndInserts(List.of(other, sub));

        // act
        CategoryBatchResultDto result = service.createBatch(USER_ID, request(group(FOOD, "Misc")));

        // assert & verify
        assertEquals(2, result.createdCount());
        assertTrue(result.skipped().isEmpty());
    }

    @Test
    @DisplayName("should create duplicate names inside the request once and report the extras")
    void duplicatesInRequestCreatedOnce() {
        // arrange
        stubUserAndInserts(List.of());

        // act
        CategoryBatchResultDto result = service.createBatch(USER_ID,
                request(group(FOOD, GROCERIES, "groceries"), group("FOOD", "Dining")));

        // assert & verify
        assertEquals(3, result.createdCount());
        assertEquals(1, result.createdParents());
        assertEquals(2, result.skipped().size());
        assertEquals("Duplicate in request", result.skipped().get(0).reason());
        assertEquals("Duplicate in request", result.skipped().get(1).reason());
        verify(categoryRepository, times(3)).insert(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("should reject an over-long name and name the value")
    void rejectsOverLongName() {
        // arrange
        String longName = "x".repeat(51);

        // act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createBatch(USER_ID, request(group(FOOD, longName))));

        // assert & verify
        assertTrue(ex.getMessage().contains(longName));
        verify(categoryRepository, never()).insert(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("should reject a blank parent name")
    void rejectsBlankParent() {
        // act & assert & verify
        assertThrows(IllegalArgumentException.class, () -> service.createBatch(USER_ID, request(group("  "))));
        verify(categoryRepository, never()).insert(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("should reject more than 50 groups")
    void rejectsTooManyGroups() {
        // arrange
        List<CategoryBatchGroup> groups = IntStream.range(0, 51).mapToObj(i -> group("G" + i)).toList();

        // act & assert & verify
        assertThrows(IllegalArgumentException.class,
                () -> service.createBatch(USER_ID, new CategoryBatchRequest(groups)));
        verify(categoryRepository, never()).insert(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("should reject more than 300 names in total")
    void rejectsTooManyNames() {
        // arrange
        List<String> subs = new ArrayList<>();
        IntStream.range(0, 300).forEach(i -> subs.add("S" + i));
        CategoryBatchGroup big = new CategoryBatchGroup("Big", EXPENSE, null, null, subs);

        // act & assert & verify
        assertThrows(IllegalArgumentException.class, () -> service.createBatch(USER_ID, request(big)));
        verify(categoryRepository, never()).insert(any(CategoryCreateRequest.class));
    }

    @Test
    @DisplayName("should reject an unknown category type")
    void rejectsInvalidType() {
        // arrange
        CategoryBatchGroup bad = new CategoryBatchGroup(FOOD, "NOPE", null, null, List.of());

        // act & assert & verify
        assertThrows(IllegalArgumentException.class, () -> service.createBatch(USER_ID, request(bad)));
    }
}
