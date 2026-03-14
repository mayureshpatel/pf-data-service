package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.category.CategoryCreateRequest;
import com.mayureshpatel.pfdataservice.dto.category.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.category.CategoryUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/categories", "/api/categories"})
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getCategoriesByUserId(userDetails.getId()));
    }

    @GetMapping("/parents")
    public ResponseEntity<List<CategoryDto>> getParentCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getParentCategories(userDetails.getId()));
    }

    @GetMapping("/children")
    public ResponseEntity<List<CategoryDto>> getChildCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getChildCategories(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<Integer> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryCreateRequest request) {
        return ResponseEntity.status(201).body(categoryService.createCategory(userDetails.getId(), request));
    }

    @PutMapping
    @PreAuthorize("@ss.isCategoryOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryUpdateRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(userDetails.getId(), request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isCategoryOwner(#id, principal)")
    public ResponseEntity<Integer> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.deleteCategory(userDetails.getId(), id));
    }
}
