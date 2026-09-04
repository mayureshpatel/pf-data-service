package com.mayureshpatel.pfdataservice.controller;

import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagDto;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagUpdateRequest;
import com.mayureshpatel.pfdataservice.security.CustomUserDetails;
import com.mayureshpatel.pfdataservice.service.TagService;
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
 * User-defined labels that can be attached to transactions, independent of category. Rules are
 * matched by (user_id, name) uniqueness at the schema level; this controller enforces ownership
 * on every operation.
 */
@Tag(name = "Tags", description = "User-defined transaction labels: CRUD and transaction assignment")
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * Returns the authenticated user's tags.
     *
     * @param userDetails the authenticated user
     * @return the user's tags
     */
    @Operation(summary = "List tags", description = "Returns the authenticated user's tags")
    @ApiResponse(responseCode = "200", description = "Tags returned (possibly empty)")
    @GetMapping
    public ResponseEntity<List<TagDto>> getTags(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(tagService.getTags(userDetails.getId()));
    }

    /**
     * Creates a new tag.
     *
     * @param userDetails the authenticated user
     * @param request     the tag to create
     * @return the new tag's generated id
     */
    @Operation(summary = "Create a tag", description = "Creates a new tag for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Tag created, id returned")
    @PostMapping
    public ResponseEntity<Long> createTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid TagCreateRequest request) {
        return ResponseEntity.ok(tagService.createTag(userDetails.getId(), request));
    }

    /**
     * Renames/recolors an existing tag. Ownership-guarded: the tag's own id doesn't prove the
     * caller owns it, so this is checked separately from authentication.
     *
     * @param userDetails the authenticated user
     * @param request     the tag to update, including its id
     * @return the number of rows affected
     */
    @Operation(summary = "Update a tag", description = "Renames/recolors a tag owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Tag updated")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this tag")
    @PutMapping
    @PreAuthorize("@ss.isTagOwner(#request.id, principal)")
    public ResponseEntity<Integer> updateTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid TagUpdateRequest request) {
        return ResponseEntity.ok(tagService.updateTag(userDetails.getId(), request));
    }

    /**
     * Deletes a tag by id. Ownership-guarded the same way as {@link #updateTag}.
     *
     * @param userDetails the authenticated user
     * @param id          the tag id to delete
     * @return 204 once deleted
     */
    @Operation(summary = "Delete a tag", description = "Deletes a tag owned by the authenticated user")
    @ApiResponse(responseCode = "204", description = "Tag deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own this tag")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.isTagOwner(#id, principal)")
    public ResponseEntity<Void> deleteTag(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        tagService.deleteTag(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assigns a tag to a transaction. Both must be owned by the caller.
     *
     * @param userDetails   the authenticated user
     * @param tagId         the tag to assign
     * @param transactionId the transaction to assign it to
     * @return 204 once assigned
     */
    @Operation(summary = "Assign a tag to a transaction", description = "Attaches a tag the caller owns to a transaction the caller owns")
    @ApiResponse(responseCode = "204", description = "Tag assigned")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own the tag and/or the transaction")
    @PostMapping("/{tagId}/transactions/{transactionId}")
    @PreAuthorize("@ss.isTagOwner(#tagId, principal) and @ss.isTransactionOwner(#transactionId, principal)")
    public ResponseEntity<Void> assignToTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tagId,
            @PathVariable Long transactionId) {
        tagService.assignToTransaction(userDetails.getId(), tagId, transactionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a tag from a transaction. Both must be owned by the caller, same as
     * {@link #assignToTransaction}.
     *
     * @param userDetails   the authenticated user
     * @param tagId         the tag to remove
     * @param transactionId the transaction to remove it from
     * @return 204 once removed
     */
    @Operation(summary = "Remove a tag from a transaction", description = "Detaches a tag the caller owns from a transaction the caller owns")
    @ApiResponse(responseCode = "204", description = "Tag removed")
    @ApiResponse(responseCode = "403", description = "Forbidden -- the authenticated user does not own the tag and/or the transaction")
    @DeleteMapping("/{tagId}/transactions/{transactionId}")
    @PreAuthorize("@ss.isTagOwner(#tagId, principal) and @ss.isTransactionOwner(#transactionId, principal)")
    public ResponseEntity<Void> removeFromTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long tagId,
            @PathVariable Long transactionId) {
        tagService.removeFromTransaction(userDetails.getId(), tagId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
