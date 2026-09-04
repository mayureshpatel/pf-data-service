package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagDto;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.mapper.TagDtoMapper;
import com.mayureshpatel.pfdataservice.repository.tag.TagRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for a user's tags, plus assigning/removing a tag on a specific transaction. Delegates
 * storage to {@link TagRepository}, including the transaction_tags join-table operations, which
 * live there rather than on {@link TransactionRepository} since they're tag-domain functionality
 * that this class is what actually exposes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Get all tags for a user.
     *
     * @param userId the user id
     * @return the user's tags
     */
    public List<TagDto> getTags(Long userId) {
        return tagRepository.findAllByUserId(userId).stream()
                .map(TagDtoMapper::toDto)
                .toList();
    }

    /**
     * Creates a new tag for a user.
     *
     * @param userId  the user id
     * @param request the tag to create
     * @return the newly created tag's generated id
     */
    @Transactional
    public Long createTag(Long userId, TagCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tag tag = Tag.builder()
                .userId(user.getId())
                .name(request.getName())
                .color(request.getColor())
                .audit(TableAudit.insertAudit(user))
                .build();

        return tagRepository.insertAndReturnId(tag);
    }

    /**
     * Renames/recolors an existing tag owned by the user.
     *
     * @param userId  the user id
     * @param request the updated tag, including its id
     * @return the number of rows affected
     */
    @Transactional
    public int updateTag(Long userId, TagUpdateRequest request) {
        Tag tag = tagRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));

        if (!tag.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this tag");
        }

        Tag updated = tag.toBuilder()
                .name(request.getName())
                .color(request.getColor())
                .build();

        return tagRepository.update(updated);
    }

    /**
     * Deletes a tag owned by the user. Its transaction_tags rows are removed automatically
     * (ON DELETE CASCADE on tag_id), not by this method.
     *
     * @param userId the user id
     * @param tagId  the tag id to delete
     */
    @Transactional
    public void deleteTag(Long userId, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));

        if (!tag.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this tag");
        }

        tagRepository.deleteById(tagId, userId);
    }

    /**
     * Assigns a tag to a transaction. Both must be owned by the caller -- ownership of neither is
     * implied by owning the other.
     *
     * @param userId        the user id
     * @param tagId         the tag to assign
     * @param transactionId the transaction to assign it to
     */
    @Transactional
    public void assignToTransaction(Long userId, Long tagId, Long transactionId) {
        tagRepository.findById(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        transactionRepository.findById(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        tagRepository.insertTransactionTag(transactionId, tagId);
    }

    /**
     * Removes a tag from a transaction. Both must be owned by the caller, same as
     * {@link #assignToTransaction}.
     *
     * @param userId        the user id
     * @param tagId         the tag to remove
     * @param transactionId the transaction to remove it from
     */
    @Transactional
    public void removeFromTransaction(Long userId, Long tagId, Long transactionId) {
        tagRepository.findById(tagId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        transactionRepository.findById(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        tagRepository.deleteTransactionTag(transactionId, tagId);
    }
}
