package qna.domain;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import qna.CannotDeleteException;
import qna.NotFoundException;

@DataJpaTest
public class QuestionRepositoryTest {
    public static final Question Q1 = new Question("title1", "contents1", UserRepositoryTest.JAVAJIGI);
    public static final Question Q2 = new Question("title2", "contents2", UserRepositoryTest.SANJIGI);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    private Question question1;
    private Question question2;

    private User user1;
    private User user2;

    @BeforeEach
    void setup() {
        user1 = new User("id1", "password1", "name1", "email1");
        user2 = new User("id2", "password2", "name2", "email2");
        entityManager.persist(user1);
        entityManager.persist(user2);

        question1 = new Question("title1", "contents1", user1);
        question2 = new Question("title2", "contents2", user2);
        entityManager.persist(question1);
        entityManager.persist(question2);
    }

    @DisplayName("저장 후 반환되는 객체 확인 테스트")
    @Test
    void save() {
        // when
        final Question actual = questionRepository.save(question1);

        // then
        assertAll(
            () -> assertThat(actual.getId()).isNotNull(),
            () -> assertThat(actual.getTitle()).isNotNull(),
            () -> assertThat(actual.getContents()).isNotNull(),
            () -> assertThat(actual.isDeleted()).isEqualTo(false),
            () -> assertThat(actual.getWriterId()).isEqualTo(user1.getId())
        );
    }

    @DisplayName("저장 후 반환되는 객체와 DB 를 조회해서 나오는 객체가 동일한지 테스트")
    @Test
    void findById() {
        // given
        final Question expected = questionRepository.save(question1);

        // when
        final Optional<Question> optActual = questionRepository.findById(expected.getId());
        final Question actual = optActual.orElseThrow(IllegalArgumentException::new);

        // then
        assertAll(
            () -> assertThat(actual.getId()).isNotNull(),
            () -> assertThat(actual.getTitle()).isNotNull(),
            () -> assertThat(actual.getContents()).isNotNull(),
            () -> assertThat(actual.isDeleted()).isEqualTo(false),
            () -> assertThat(actual.getWriterId()).isEqualTo(user1.getId()),
            () -> assertThat(actual).isEqualTo(expected),
            () -> assertThat(actual).isEqualTo(expected)
        );
    }

    @DisplayName("메소드명에 조회 조건을 부여한 새로운 메소드(findByIdAndDeletedTrue)를 만들었을 때 정상적으로 조회되는지 확인하는 테스트")
    @Test
    void findByIdAndDeletedTrue() throws CannotDeleteException {
        // given
        final Question questionMarkedDeleted = questionRepository.save(question1);
        questionMarkedDeleted.delete(user1);
        final Question questionMarkUndeleted = questionRepository.save(question2);

        // when
        final Question actualMarkDeleted = questionRepository.findByIdAndDeletedTrue(questionMarkedDeleted.getId())
            .orElseThrow(IllegalArgumentException::new);
        final Optional<Question> optNotExistActualMarkDeleted = questionRepository.findByIdAndDeletedFalse(
            questionMarkedDeleted.getId());

        final Question actualMarkUndeleted = questionRepository.findByIdAndDeletedFalse(questionMarkUndeleted.getId())
            .orElseThrow(IllegalArgumentException::new);
        final Optional<Question> optNotExistActualMarkUndeletedNotExist = questionRepository.findByIdAndDeletedTrue(
            questionMarkUndeleted.getId());

        // then
        assertAll(
            () -> assertThat(actualMarkDeleted).isNotNull(),
            () -> assertThat(actualMarkDeleted.getId()).isNotNull(),
            () -> assertThat(actualMarkDeleted).isEqualTo(questionMarkedDeleted),
            () -> assertThat(actualMarkDeleted).isEqualTo(questionMarkedDeleted),
            () -> assertThat(actualMarkUndeleted).isNotNull(),
            () -> assertThat(actualMarkUndeleted.getId()).isNotNull(),
            () -> assertThat(actualMarkUndeleted).isEqualTo(questionMarkUndeleted),
            () -> assertThat(actualMarkUndeleted).isEqualTo(questionMarkUndeleted),
            () -> assertThatThrownBy(
                () -> optNotExistActualMarkDeleted.orElseThrow(IllegalArgumentException::new)).isInstanceOf(
                IllegalArgumentException.class),
            () -> assertThatThrownBy(() -> optNotExistActualMarkUndeletedNotExist.orElseThrow(
                IllegalArgumentException::new)).isInstanceOf(IllegalArgumentException.class)
        );
    }

    @DisplayName("영속 상태와 비영속 상태에 있는 ID를 각각 조회하는 테스트")
    @Test
    void existsById() {
        // given
        final Question savedQuestion = questionRepository.save(question1);
        final Long persistentId = savedQuestion.getId();
        final long notPersistentId = 2L;

        // when
        final boolean actualTrue = questionRepository.existsById(persistentId);
        final boolean actualFalse = questionRepository.existsById(notPersistentId);

        // then
        assertAll(
            () -> assertThat(actualTrue).isEqualTo(true),
            () -> assertThat(actualFalse).isEqualTo(false)
        );
    }

    @DisplayName("deteled = true 인 조건만 검색하는 테스트")
    @Test
    void findByDeletedFalse() throws CannotDeleteException {
        // given
        final Question savedQuestion1 = questionRepository.save(question1);
        final Question savedQuestion2 = questionRepository.save(question2);
        savedQuestion2.delete(user2);

        // when
        final List<Question> actual = questionRepository.findByDeletedFalse();

        // then
        assertAll(
            () -> assertThat(actual.size()).isEqualTo(1),
            () -> assertThat(actual.get(0)).isEqualTo(savedQuestion1)
        );
    }

    @DisplayName("@Query 으로 Title 필드를 조건으로 검색하는 메소드 테스트")
    @Test
    void findByTitle() {
        // given
        questionRepository.save(question1);
        questionRepository.save(question2);

        // when
        final List<Question> actual = questionRepository.findByTitle(question1.getTitle());

        // then
        assertAll(
            () -> assertThat(actual).isNotNull(),
            () -> assertThat(actual.size()).isEqualTo(1),
            () -> assertThat(actual.get(0)).isEqualTo(question1)
        );
    }

    @DisplayName("contents 필드 contains 검색 테스트")
    @Test
    void findByContentContains() {
        // given
        userRepository.save(UserRepositoryTest.JAVAJIGI);
        userRepository.save(UserRepositoryTest.SANJIGI);
        questionRepository.save(question1);
        questionRepository.save(question2);
        final String criteria = "contents";

        // when
        final List<Question> actual = questionRepository.findByContentsContains(criteria);

        // then
        assertAll(
            () -> assertThat(actual).isNotNull(),
            () -> assertThat(actual.size()).isEqualTo(2),
            () -> assertThat(actual).containsExactly(question1, question2)
        );
    }

    @DisplayName("Question 과 Answer 를 연관 관계 매핑을 하고 정상적으로 저장되는지 확인하는 테스트")
    @Test
    void findByIdAndDeletedFalse() {
        // given
        final Answer answer1 = new Answer(user1, question1, "contents");
        final Answer answer2 = new Answer(user1, question1, "contents2");
        question1.addAnswer(answer1);
        question1.addAnswer(answer2);

        // when
        final Question actual = questionRepository.findByIdAndDeletedFalse(question1.getId())
            .orElseThrow(NotFoundException::new);

        // then
        assertAll(
            () -> assertThat(actual).isNotNull(),
            () -> assertThat(actual.isDeleted()).isEqualTo(false),
            () -> assertThat(actual.countOfAnswer()).isEqualTo(2)
        );
    }
}
