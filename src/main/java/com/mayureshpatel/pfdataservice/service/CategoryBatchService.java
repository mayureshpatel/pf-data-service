package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.category.Category;
import com.mayureshpatel.pfdataservice.domain.category.CategoryType;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchGroup;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchResultDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryBatchSkippedDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.category.CategoryRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Creates many categories in one atomic call (PF-897), so a new user can set up parents and
 * subcategories in one step. Kept apart from {@link CategoryService} to keep that class small.
 * Names that already exist for the user are skipped and reported rather than duplicated: a parent
 * that exists is reused (its subcategories are added under it, its own type, color and icon are
 * left alone). Nothing is persisted if any insert fails, because the whole call is one transaction.
 */
@Service
@RequiredArgsConstructor
public class CategoryBatchService {

    static final String REASON_EXISTS = "Already exists";
    static final String REASON_DUPLICATE = "Duplicate in request";
    private static final String DEFAULT_TYPE = CategoryType.EXPENSE.name();

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    /**
     * Creates the requested parents and subcategories for the user. The user's categories are
     * loaded once, so the number of queries stays constant apart from one insert per new category.
     *
     * @param userId  the owning user's id, taken from the principal
     * @param request the parents and subcategory names to create
     * @return counts of what was created plus every skipped name with its reason
     * @throws IllegalArgumentException if a name is too long, a parent name is blank, or a limit is exceeded
     */
    @Transactional
    public CategoryBatchResultDto createBatch(Long userId, CategoryBatchRequest request) {
        validate(request);
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BatchState state = new BatchState();
        indexExisting(categoryRepository.findByUserId(userId), state);
        request.groups().forEach(group -> processGroup(userId, group, state));

        return new CategoryBatchResultDto(state.createdParents + state.createdSubs, state.createdParents,
                state.createdSubs, state.skipped);
    }

    /** Creates or reuses one parent, then adds its subcategories under it. */
    private void processGroup(Long userId, CategoryBatchGroup group, BatchState state) {
        String parentName = group.name().trim();
        Category parent = state.parentsByName.get(key(parentName));

        if (parent == null) {
            parent = insertParent(userId, group, parentName);
            state.parentsByName.put(key(parentName), parent);
            state.createdParentIds.add(parent.getId());
            state.createdParents++;
        } else {
            // a parent created earlier in this same request is a duplicate, not a pre-existing one
            state.skip(parentName, null, state.createdParentIds.contains(parent.getId()));
        }

        Set<String> known = state.subNamesByParent.computeIfAbsent(parent.getId(), id -> new HashSet<>());
        for (String raw : group.subcategories() == null ? List.<String>of() : group.subcategories()) {
            if (raw != null && !raw.isBlank()) {
                processSub(userId, parent, raw.trim(), known, state);
            }
        }
    }

    /** Creates one subcategory unless the parent already has (or was just given) that name. */
    private void processSub(Long userId, Category parent, String subName, Set<String> known, BatchState state) {
        String subKey = parent.getId() + "|" + key(subName);
        if (!known.add(key(subName))) {
            state.skip(subName, parent.getName(), state.createdSubKeys.contains(subKey));
            return;
        }
        state.createdSubKeys.add(subKey);
        insertSub(userId, parent, subName);
        state.createdSubs++;
    }

    /** Rejects blank parents, over-long names, unknown types and over-sized batches. */
    private void validate(CategoryBatchRequest request) {
        if (request == null || request.groups() == null || request.groups().isEmpty()) {
            throw new IllegalArgumentException("At least one category group is required.");
        }
        if (request.groups().size() > CategoryBatchRequest.MAX_GROUPS) {
            throw new IllegalArgumentException("A batch can contain at most "
                    + CategoryBatchRequest.MAX_GROUPS + " category groups.");
        }
        if (request.totalNames() > CategoryBatchRequest.MAX_NAMES) {
            throw new IllegalArgumentException("A batch can contain at most "
                    + CategoryBatchRequest.MAX_NAMES + " category names in total.");
        }
        request.groups().forEach(this::validateGroup);
    }

    private void validateGroup(CategoryBatchGroup group) {
        if (group == null || group.name() == null || group.name().isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank.");
        }
        requireLength(group.name());
        if (group.subcategories() != null) {
            group.subcategories().forEach(this::requireLength);
        }
        if (group.type() != null && !group.type().isBlank()) {
            CategoryType.fromValue(group.type().trim());
        }
    }

    private void requireLength(String name) {
        if (name != null && name.trim().length() > CategoryBatchRequest.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Category name '" + name.trim() + "' must be at most "
                    + CategoryBatchRequest.MAX_NAME_LENGTH + " characters.");
        }
    }

    /** Splits the user's categories into top-level ones by name and sub names by parent id. */
    private void indexExisting(List<Category> existing, BatchState state) {
        for (Category category : existing) {
            if (category.isSubCategory()) {
                state.subNamesByParent.computeIfAbsent(category.getParentId(), id -> new HashSet<>())
                        .add(key(category.getName()));
            } else {
                state.parentsByName.putIfAbsent(key(category.getName()), category);
            }
        }
    }

    private Category insertParent(Long userId, CategoryBatchGroup group, String name) {
        String type = group.type() == null || group.type().isBlank() ? DEFAULT_TYPE : group.type().trim();
        CategoryCreateRequest insert = CategoryCreateRequest.builder()
                .userId(userId).name(name).type(type).color(group.color()).icon(group.icon()).build();
        long id = categoryRepository.insert(insert);
        return Category.builder().id(id).userId(userId).name(name).type(type)
                .color(group.color()).icon(group.icon()).build();
    }

    private void insertSub(Long userId, Category parent, String name) {
        String type = parent.getType() == null ? DEFAULT_TYPE : parent.getType();
        categoryRepository.insert(CategoryCreateRequest.builder()
                .userId(userId).name(name).type(type).color(parent.getColor()).icon(parent.getIcon())
                .parentId(parent.getId()).build());
    }

    /** Case-insensitive, trimmed comparison key for a category name. */
    private String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    /** The mutable bookkeeping of one batch run: known names, what was created and what was skipped. */
    private static final class BatchState {
        private final Map<String, Category> parentsByName = new HashMap<>();
        private final Map<Long, Set<String>> subNamesByParent = new HashMap<>();
        private final Set<Long> createdParentIds = new HashSet<>();
        private final Set<String> createdSubKeys = new HashSet<>();
        private final List<CategoryBatchSkippedDto> skipped = new ArrayList<>();
        private int createdParents;
        private int createdSubs;

        private void skip(String name, String parentName, boolean duplicateInRequest) {
            skipped.add(new CategoryBatchSkippedDto(name, parentName,
                    duplicateInRequest ? REASON_DUPLICATE : REASON_EXISTS));
        }
    }
}
