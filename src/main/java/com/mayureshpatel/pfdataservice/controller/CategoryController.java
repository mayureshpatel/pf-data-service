package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.CategoryDto;
import com.mayureshpatel.pfdataservice.dto.CategoryGroupDto;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/grouped")
    public ResponseEntity<List<CategoryGroupDto>> getCategoriesGrouped(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getCategoriesGrouped(userDetails.getId()));
    }

    @GetMapping("/children")
    public ResponseEntity<List<CategoryDto>> getChildCategories(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(categoryService.getChildCategories(userDetails.getId()));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid CategoryDto categoryDto) {
        return ResponseEntity.ok(categoryService.createCategory(userDetails.getId(), categoryDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody @Valid CategoryDto categoryDto) {
        return ResponseEntity.ok(categoryService.updateCategory(userDetails.getId(), id, categoryDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        categoryService.deleteCategory(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
