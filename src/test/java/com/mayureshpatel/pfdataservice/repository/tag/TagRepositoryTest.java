package com.mayureshpatel.pfdataservice.repository.tag;

import com.mayureshpatel.pfdataservice.BaseIntegrationTest;
import com.mayureshpatel.pfdataservice.domain.TimestampAudit;
import com.mayureshpatel.pfdataservice.domain.transaction.Tag;
import com.mayureshpatel.pfdataservice.domain.user.User;
import com.mayureshpatel.pfdataservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("TagRepository Integration Tests")
class TagRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private TestDataFactory factory;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = factory.createUser("tag_" + System.currentTimeMillis());
    }

    private Tag createTag(String name, String color) {
        Tag tag = new Tag();
        tag.setUser(testUser);
        tag.setName(name);
        tag.setColor(color);
        tag.setAudit(new TimestampAudit());
        return tagRepository.save(tag);
    }

    @Test
    @DisplayName("save() should insert new tag and assign ID")
    void save_shouldInsertTag() {
        Tag tag = createTag("Urgent", "#FF0000");

        assertThat(tag.getId()).isNotNull();
    }

    @Test
    @DisplayName("findById() should return tag when exists")
    void findById_shouldReturnTag() {
        Tag saved = createTag("Important", "#0000FF");

        Optional<Tag> found = tagRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Important");
        assertThat(found.get().getColor()).isEqualTo("#0000FF");
    }

    @Test
    @DisplayName("findById() should return empty when not exists")
    void findById_shouldReturnEmpty() {
        assertThat(tagRepository.findById(99999L)).isEmpty();
    }

    @Test
    @DisplayName("findAllByUserId() should return all tags for user")
    void findAllByUserId_shouldReturnUserTags() {
        createTag("Tag1", "#111111");
        createTag("Tag2", "#222222");

        List<Tag> tags = tagRepository.findAllByUserId(testUser.getId());

        assertThat(tags).hasSize(2);
        assertThat(tags).extracting(Tag::getName).containsExactlyInAnyOrder("Tag1", "Tag2");
    }

    @Test
    @DisplayName("findAllByUserId() should return empty when user has no tags")
    void findAllByUserId_shouldReturnEmpty() {
        User otherUser = factory.createUser("notags_" + System.currentTimeMillis());

        List<Tag> tags = tagRepository.findAllByUserId(otherUser.getId());

        assertThat(tags).isEmpty();
    }

    @Test
    @DisplayName("update() should modify existing tag")
    void update_shouldModifyTag() {
        Tag tag = createTag("Old", "#000000");

        tag.setName("New");
        tag.setColor("#FFFFFF");
        tagRepository.update(tag);

        Optional<Tag> found = tagRepository.findById(tag.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("New");
        assertThat(found.get().getColor()).isEqualTo("#FFFFFF");
    }

    @Test
    @DisplayName("delete() should remove tag")
    void delete_shouldRemoveTag() {
        Tag tag = createTag("ToDelete", "#FF0000");

        tagRepository.delete(tag);

        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    @DisplayName("deleteById() should remove tag by ID")
    void deleteById_shouldRemoveTag() {
        Tag tag = createTag("DeleteById", "#00FF00");

        tagRepository.deleteById(tag.getId());

        assertThat(tagRepository.findById(tag.getId())).isEmpty();
    }

    @Test
    @DisplayName("data isolation - user cannot see other user's tags")
    void dataIsolation_shouldIsolateByUser() {
        createTag("User1Tag", "#111111");

        User otherUser = factory.createUser("othertag_" + System.currentTimeMillis());
        Tag otherTag = new Tag();
        otherTag.setUser(otherUser);
        otherTag.setName("User2Tag");
        otherTag.setColor("#222222");
        otherTag.setAudit(new TimestampAudit());
        tagRepository.save(otherTag);

        List<Tag> user1Tags = tagRepository.findAllByUserId(testUser.getId());
        List<Tag> user2Tags = tagRepository.findAllByUserId(otherUser.getId());

        assertThat(user1Tags).extracting(Tag::getName).containsExactly("User1Tag");
        assertThat(user2Tags).extracting(Tag::getName).containsExactly("User2Tag");
    }
}
