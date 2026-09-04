package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD for the authenticated user's categories, which form a two-level hierarchy (parent
 * categories with optional subcategories).
 */
@Tag(name = "Categories", description = "The authenticated user's category hierarchy")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Returns all categories for the authenticated user, flat (not grouped by parent).
     *
     * @param userDetails the authenticated user
     * @return the user's categories
     */
    @Operation(summary = "List categories", description = "Returns all categories for the authenticated user, flat")
    @ApiResponse(responseCode = "200", description = "Categories returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CategoryDto> categories = categoryService.getCategoriesByUserId(userDetails.getId());
        return ResponseEntity.ok(categories);
    }

    /**
     * Returns only the user's top-level (parent) categories.
     *
     * @param userDetails the authenticated user
     * @return the user's parent categories
     */
    @Operation(summary = "List parent categories", description = "Returns only the user's top-level categories")
    @ApiResponse(responseCode = "200", description = "Parent categories returned (possibly empty)")
    @GetMapping("/parents")
    public ResponseEntity<List<CategoryDto>> getParentCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getParentCategories(userDetails.getId()));
    }

    /**
     * Returns only the user's subcategories (categories with a parent).
     *
     * @param userDetails the authenticated user
     * @return the user's subcategories
     */
    @Operation(summary = "List subcategories", description = "Returns only the user's subcategories")
    @ApiResponse(responseCode = "200", description = "Subcategories returned (possibly empty)")
    @GetMapping("/children")
    public ResponseEntity<List<CategoryDto>> getChildCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getChildCategories(userDetails.getId()));
    }

    /**
     * Creates a new category.
     *
     * @param userDetails the authenticated user
     * @param request     the category to create
     * @return 201 with the new category's generated id
     */
    @Operation(summary = "Create a category", description = "Creates a new category for the authenticated user")
    @ApiResponse(responseCode = "201", description = "Category created, id returned")
    @PostMapping
    public ResponseEntity<Integer> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryCreateRequest request) {
        return ResponseEntity.status(201).body(categoryService.createCategory(userDetails.getId(), request));
    }

    /**
     * Updates an existing category. Ownership-guarded: the category's own id doesn't prove the
     * caller owns it, so this is checked separately from authentication.
     *
     * @param userDetails the authenticated user
     * @param request     the category to update, including its id
     * @return the updated category's id
     */
    @Operation(summary = "Update a category", description = "Updates a category owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Category updated, id returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this category")
    @PutMapping
    @PreAuthorize("@ss.isCategoryOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(userDetails.getId(), request));
    }

    /**
     * Deletes a category by id. Ownership-guarded the same way as {@link #updateCategory}.
     *
     * @param userDetails the authenticated user
     * @param id          the category id to delete
     * @return the number of rows deleted
     */
    @Operation(summary = "Delete a category", description = "Deletes a category owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Rows-deleted count returned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this category")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isCategoryOwner(#id, principal)")
    public ResponseEntity<Integer> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.deleteCategory(userDetails.getId(), id));
    }
}
