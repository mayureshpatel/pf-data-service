package com.mayureshpatel.pfdataservice.domain.user;

import com.mayureshpatel.pfdataservice.domain.TableAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Domain Object Tests")
class UserTest {

    @Test
    @DisplayName("Builder should create User with correct fields")
    void builder_shouldCreateUser() {
        // Arrange
        TableAudit audit = TableAudit.insertAudit(null);
        
        // Act
        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .passwordHash("hashed_pass")
                .audit(audit)
                .build();

        // Assert
        assertEquals(1L, user.getId());
        assertEquals("john_doe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("hashed_pass", user.getPasswordHash());
        assertEquals(audit, user.getAudit());
    }

    @Test
    @DisplayName("toBuilder should create a copy that can be modified")
    void toBuilder_shouldCreateMutableCopy() {
        // Arrange
        User original = User.builder()
                .id(1L)
                .username("john_doe")
                .build();

        // Act
        User modified = original.toBuilder()
                .username("jane_doe")
                .build();

        // Assert
        assertNotSame(original, modified);
        assertEquals(1L, modified.getId());
        assertEquals("jane_doe", modified.getUsername());
        assertEquals("john_doe", original.getUsername());
    }

    @Test
    @DisplayName("User equality should be based strictly on ID")
    void equality_shouldBeBasedOnId() {
        User u1 = User.builder().id(1L).username("user1").build();
        User u2 = User.builder().id(1L).username("user2").build();
        User u3 = User.builder().id(2L).username("user1").build();

        assertEquals(u1, u2, "Users with same ID must be equal");
        assertNotEquals(u1, u3, "Users with different ID must not be equal");
        assertEquals(u1.hashCode(), u2.hashCode(), "Equal users must have same hash code");
    }

    @Test
    @DisplayName("ToString should contain username but exclude audit")
    void toString_shouldBeInformative() {
        User user = User.builder().username("testuser").build();
        String toString = user.toString();
        
        assertTrue(toString.contains("testuser"));
        assertFalse(toString.contains("audit"), "ToString should exclude audit field");
    }
}
