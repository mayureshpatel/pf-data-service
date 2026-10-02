package com.mayureshpatel.pfdataservice.service;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.transaction.Transaction;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagCreateRequest;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagDto;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagUpdateRequest;
import com.mayureshpatel.pfdataservice.exception.ResourceNotFoundException;
import com.mayureshpatel.pfdataservice.repository.tag.TagRepository;
import com.mayureshpatel.pfdataservice.repository.transaction.TransactionRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Verifies {@code TagService}'s CRUD and its transaction-assignment methods, one {@code @Nested} class per method below. */
@ExtendWith(MockitoExtension.class)
@DisplayName("TagService Unit Tests")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TagService tagService;

    private static final Long USER_ID = 1L;
    private static final Long TAG_ID = 100L;
    private static final Long TRANSACTION_ID = 200L;
    private static final String TAG_NAME_TRAVEL = "Travel";
    private static final String TAG_NAME_NEW = "New";

    /** {@code getTags} maps every repository row for the user to a {@link TagDto}. */
    @Nested
    @DisplayName("getTags")
    class GetTagsTests {
        @Test
        @DisplayName("should return list of tag DTOs")
        void shouldReturnTags() {
            // arrange
            Tag tag = Tag.builder().id(TAG_ID).userId(USER_ID).name(TAG_NAME_TRAVEL).color("#123").build();
            when(tagRepository.findAllByUserId(USER_ID)).thenReturn(List.of(tag));

            // act
            List<TagDto> result = tagService.getTags(USER_ID);

            // assert & verify
            assertEquals(1, result.size());
            assertEquals(TAG_NAME_TRAVEL, result.get(0).name());
        }
    }

    /** {@code createTag} requires the user to exist ({@link ResourceNotFoundException}) before passing the request's fields through to the inserted tag. */
    @Nested
    @DisplayName("createTag")
    class CreateTagTests {
        @Test
        @DisplayName("should create tag successfully")
        void shouldCreate() {
            // arrange
            User user = User.builder().id(USER_ID).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(tagRepository.insertAndReturnId(any())).thenReturn(42L);

            TagCreateRequest request = TagCreateRequest.builder()
                    .userId(USER_ID)
                    .name(TAG_NAME_TRAVEL)
                    .color("#123456")
                    .build();

            // act
            Long result = tagService.createTag(USER_ID, request);

            // assert & verify
            assertEquals(42L, result);
            verify(tagRepository).insertAndReturnId(argThat(t ->
                    TAG_NAME_TRAVEL.equals(t.getName()) && "#123456".equals(t.getColor()) && t.getUserId().equals(USER_ID)));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if user not found")
        void shouldThrowOnUserNotFound() {
            // arrange
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            TagCreateRequest request = TagCreateRequest.builder().userId(USER_ID).name(TAG_NAME_TRAVEL).build();

            // act & assert
            assertThrows(ResourceNotFoundException.class, () -> tagService.createTag(USER_ID, request));
        }
    }

    /** {@code updateTag} requires the tag to exist ({@link ResourceNotFoundException}) and be owned by the caller ({@link AccessDeniedException}) before delegating to the repository. */
    @Nested
    @DisplayName("updateTag")
    class UpdateTagTests {
        @Test
        @DisplayName("should update tag successfully")
        void shouldUpdate() {
            // arrange
            Tag existing = Tag.builder().id(TAG_ID).userId(USER_ID).name("Old").color("#000").build();
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(existing));
            when(tagRepository.update(any())).thenReturn(1);

            TagUpdateRequest request = TagUpdateRequest.builder().id(TAG_ID).name(TAG_NAME_NEW).color("#FFF").build();

            // act
            int result = tagService.updateTag(USER_ID, request);

            // assert & verify
            assertEquals(1, result);
            verify(tagRepository).update(argThat(t -> TAG_NAME_NEW.equals(t.getName()) && "#FFF".equals(t.getColor())));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if tag not found")
        void shouldThrowOnTagNotFound() {
            // arrange
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.empty());
            TagUpdateRequest request = TagUpdateRequest.builder().id(TAG_ID).name(TAG_NAME_NEW).build();

            // act & assert
            assertThrows(ResourceNotFoundException.class, () -> tagService.updateTag(USER_ID, request));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own the tag")
        void shouldThrowOnOwnership() {
            // arrange
            Tag existing = Tag.builder().id(TAG_ID).userId(999L).name("Old").build();
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(existing));
            TagUpdateRequest request = TagUpdateRequest.builder().id(TAG_ID).name(TAG_NAME_NEW).build();

            // act & assert
            assertThrows(AccessDeniedException.class, () -> tagService.updateTag(USER_ID, request));
        }
    }

    /** {@code deleteTag} requires the tag to exist ({@link ResourceNotFoundException}) and be owned by the caller ({@link AccessDeniedException}) before delegating to the repository. */
    @Nested
    @DisplayName("deleteTag")
    class DeleteTagTests {
        @Test
        @DisplayName("should delete tag successfully")
        void shouldDelete() {
            // arrange
            Tag tag = Tag.builder().id(TAG_ID).userId(USER_ID).build();
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));

            // act
            tagService.deleteTag(USER_ID, TAG_ID);

            // assert & verify
            verify(tagRepository).deleteById(TAG_ID, USER_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if tag not found")
        void shouldThrowOnTagNotFound() {
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class, () -> tagService.deleteTag(USER_ID, TAG_ID));
        }

        @Test
        @DisplayName("should throw AccessDeniedException if user does not own the tag")
        void shouldThrowOnOwnership() {
            Tag tag = Tag.builder().id(TAG_ID).userId(999L).build();
            when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
            assertThrows(AccessDeniedException.class, () -> tagService.deleteTag(USER_ID, TAG_ID));
        }
    }

    /** {@code assignToTransaction} requires both the tag and the transaction to be owned by the caller (each independently checked via their own user-scoped {@code findById} overload) before inserting the join row, throwing {@link ResourceNotFoundException} for whichever one fails ownership. */
    @Nested
    @DisplayName("assignToTransaction")
    class AssignToTransactionTests {
        @Test
        @DisplayName("should assign when both the tag and transaction are owned by the user")
        void shouldAssign() {
            // arrange
            Tag tag = Tag.builder().id(TAG_ID).userId(USER_ID).build();
            Transaction transaction = Transaction.builder().id(TRANSACTION_ID).build();
            when(tagRepository.findById(TAG_ID, USER_ID)).thenReturn(Optional.of(tag));
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(transaction));

            // act
            tagService.assignToTransaction(USER_ID, TAG_ID, TRANSACTION_ID);

            // assert & verify
            verify(tagRepository).insertTransactionTag(TRANSACTION_ID, TAG_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the tag isn't owned by the user")
        void shouldThrowWhenTagNotOwned() {
            // arrange
            when(tagRepository.findById(TAG_ID, USER_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThrows(ResourceNotFoundException.class,
                    () -> tagService.assignToTransaction(USER_ID, TAG_ID, TRANSACTION_ID));
            verify(tagRepository, never()).insertTransactionTag(anyLong(), anyLong());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the transaction isn't owned by the user")
        void shouldThrowWhenTransactionNotOwned() {
            // arrange
            Tag tag = Tag.builder().id(TAG_ID).userId(USER_ID).build();
            when(tagRepository.findById(TAG_ID, USER_ID)).thenReturn(Optional.of(tag));
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());

            // act & assert
            assertThrows(ResourceNotFoundException.class,
                    () -> tagService.assignToTransaction(USER_ID, TAG_ID, TRANSACTION_ID));
            verify(tagRepository, never()).insertTransactionTag(anyLong(), anyLong());
        }
    }

    /** {@code removeFromTransaction} enforces the same dual ownership check as {@link AssignToTransactionTests} before deleting the join row. */
    @Nested
    @DisplayName("removeFromTransaction")
    class RemoveFromTransactionTests {
        @Test
        @DisplayName("should remove when both the tag and transaction are owned by the user")
        void shouldRemove() {
            // arrange
            Tag tag = Tag.builder().id(TAG_ID).userId(USER_ID).build();
            Transaction transaction = Transaction.builder().id(TRANSACTION_ID).build();
            when(tagRepository.findById(TAG_ID, USER_ID)).thenReturn(Optional.of(tag));
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.of(transaction));

            // act
            tagService.removeFromTransaction(USER_ID, TAG_ID, TRANSACTION_ID);

            // assert & verify
            verify(tagRepository).deleteTransactionTag(TRANSACTION_ID, TAG_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the tag isn't owned by the user")
        void shouldThrowWhenTagNotOwned() {
            when(tagRepository.findById(TAG_ID, USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> tagService.removeFromTransaction(USER_ID, TAG_ID, TRANSACTION_ID));
            verify(tagRepository, never()).deleteTransactionTag(anyLong(), anyLong());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when the transaction isn't owned by the user")
        void shouldThrowWhenTransactionNotOwned() {
            Tag tag = Tag.builder().id(TAG_ID).userId(USER_ID).build();
            when(tagRepository.findById(TAG_ID, USER_ID)).thenReturn(Optional.of(tag));
            when(transactionRepository.findById(TRANSACTION_ID, USER_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> tagService.removeFromTransaction(USER_ID, TAG_ID, TRANSACTION_ID));
            verify(tagRepository, never()).deleteTransactionTag(anyLong(), anyLong());
        }
    }
}
