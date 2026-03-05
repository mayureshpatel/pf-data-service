package com.mayureshpatel.pfdataservice.domain;

import com.mayureshpatel.pfdataservice.domain.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TableAudit Domain Object Tests")
class TableAuditTest {

    private final User mockUser = User.builder().id(1L).username("testuser").build();

    @Test
    @DisplayName("insertAudit should populate creation and update fields")
    void insertAudit_shouldPopulateCorrectFields() {
        // Act
        TableAudit audit = TableAudit.insertAudit(mockUser);

        // Assert
        assertNotNull(audit.getCreatedAt());
        assertNotNull(audit.getUpdatedAt());
        assertEquals(mockUser, audit.getCreatedBy());
        assertEquals(mockUser, audit.getUpdatedBy());
        assertNull(audit.getDeletedAt());
        assertNull(audit.getDeletedBy());
        
        // Verify timestamps are recent (within 1 second)
        OffsetDateTime now = OffsetDateTime.now();
        assertTrue(audit.getCreatedAt().isBefore(now.plusSeconds(1)));
        assertTrue(audit.getCreatedAt().isAfter(now.minusSeconds(5)));
    }

    @Test
    @DisplayName("updateAudit should only populate update fields")
    void updateAudit_shouldPopulateCorrectFields() {
        // Act
        TableAudit audit = TableAudit.updateAudit(mockUser);

        // Assert
        assertNull(audit.getCreatedAt());
        assertNull(audit.getCreatedBy());
        assertNotNull(audit.getUpdatedAt());
        assertEquals(mockUser, audit.getUpdatedBy());
    }

    @Test
    @DisplayName("deleteAudit should only populate deletion fields")
    void deleteAudit_shouldPopulateCorrectFields() {
        // Act
        TableAudit audit = TableAudit.deleteAudit(mockUser);

        // Assert
        assertNull(audit.getCreatedAt());
        assertNull(audit.getCreatedBy());
        assertNull(audit.getUpdatedAt());
        assertNull(audit.getUpdatedBy());
        assertNotNull(audit.getDeletedAt());
        assertEquals(mockUser, audit.getDeletedBy());
    }

    @Test
    @DisplayName("toBuilder should allow updating an existing audit while preserving creation fields")
    void toBuilder_shouldAllowPreservingFields() {
        // Arrange
        TableAudit original = TableAudit.insertAudit(mockUser);
        User updatingUser = User.builder().id(2L).username("updater").build();

        // Act
        TableAudit updated = original.toBuilder()
                .updatedAt(OffsetDateTime.now())
                .updatedBy(updatingUser)
                .build();

        // Assert
        assertEquals(original.getCreatedAt(), updated.getCreatedAt());
        assertEquals(original.getCreatedBy(), updated.getCreatedBy());
        assertNotEquals(original.getUpdatedAt(), updated.getUpdatedAt());
        assertEquals(updatingUser, updated.getUpdatedBy());
    }
}
