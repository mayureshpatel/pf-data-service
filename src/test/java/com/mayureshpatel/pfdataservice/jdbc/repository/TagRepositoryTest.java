package com.mayureshpatel.pfdataservice.jdbc.repository;

import com.mayureshpatel.pfdataservice.JdbcTestBase;
import com.mayureshpatel.pfdataservice.domain.Iconography;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.repository.tag.TagRepository;
import com.mayureshpatel.pfdataservice.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TagRepositoryTest extends JdbcTestBase {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("tag_test_user");
        user.setEmail("tag_test@example.com");
        user.setPasswordHash("hash");
        testUser = userRepository.insert(user);
    }

    private Tag buildTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setIconography(new Iconography("icon", "#FFFFFF"));
        tag.setUser(testUser);
        return tag;
    }

    @Test
    void insert_ShouldCreateTag() {
        Tag saved = tagRepository.insert(buildTag("Work"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Work");
    }

    @Test
    void findById_ShouldReturnTag() {
        Tag saved = tagRepository.insert(buildTag("Work"));

        Optional<Tag> found = tagRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Work");
    }

    @Test
    void findAllByUserId_ShouldReturnUserTags() {
        tagRepository.insert(buildTag("Work"));
        tagRepository.insert(buildTag("Personal"));

        List<Tag> tags = tagRepository.findAllByUserId(testUser.getId());

        assertThat(tags).hasSize(2);
    }

    @Test
    void update_ShouldUpdateTag() {
        Tag saved = tagRepository.insert(buildTag("Old"));
        saved.setName("New");

        Tag updated = tagRepository.update(saved);

        assertThat(updated.getName()).isEqualTo("New");
    }

    @Test
    void deleteById_ShouldRemoveTag() {
        Tag saved = tagRepository.insert(buildTag("Work"));

        tagRepository.deleteById(saved.getId());

        assertThat(tagRepository.findById(saved.getId())).isEmpty();
    }
}
