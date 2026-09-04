package com.mayureshpatel.pfdataservice.mapper;

import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.dto.transaction.tags.TagDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TagDtoMapper Unit Tests")
class TagDtoMapperTest {

    @Test
    @DisplayName("Private constructor should be accessible for coverage")
    void testPrivateConstructor() throws Exception {
        // Arrange
        Constructor<TagDtoMapper> constructor = TagDtoMapper.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        // Act
        TagDtoMapper instance = constructor.newInstance();

        // Assert
        assertNotNull(instance);
    }

    @Nested
    @DisplayName("Method: toDto")
    class ToDtoMappingTests {

        @Test
        @DisplayName("should return null when source is null")
        void toDto_shouldReturnNullWhenSourceIsNull() {
            // Act
            TagDto result = TagDtoMapper.toDto(null);

            // Assert
            assertNull(result);
        }

        @Test
        @DisplayName("should map all fields when source is fully populated")
        void toDto_shouldMapAllFields() {
            // Arrange
            Tag tag = Tag.builder()
                    .id(1L)
                    .userId(100L)
                    .name("Travel")
                    .color("#123456")
                    .build();

            // Act
            TagDto dto = TagDtoMapper.toDto(tag);

            // Assert
            assertNotNull(dto);
            assertEquals(tag.getId(), dto.id());
            assertEquals(tag.getUserId(), dto.userId());
            assertEquals(tag.getName(), dto.name());
            assertEquals(tag.getColor(), dto.color());
        }

        @Test
        @DisplayName("should handle a null color")
        void toDto_shouldHandleNullColor() {
            // Arrange
            Tag tag = Tag.builder().id(1L).userId(100L).name("Travel").color(null).build();

            // Act
            TagDto dto = TagDtoMapper.toDto(tag);

            // Assert
            assertNotNull(dto);
            assertNull(dto.color());
        }
    }
}
