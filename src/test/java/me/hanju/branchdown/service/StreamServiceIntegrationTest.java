package me.hanju.branchdown.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import me.hanju.branchdown.IntegrationTestBase;
import me.hanju.branchdown.constant.StreamConstants;
import me.hanju.branchdown.dto.PointDto;
import me.hanju.branchdown.dto.StreamDto;
import me.hanju.branchdown.entity.BranchEntity;
import me.hanju.branchdown.entity.PointEntity;
import me.hanju.branchdown.entity.StreamEntity;
import me.hanju.branchdown.fixture.StreamFixture;
import me.hanju.branchdown.repository.BranchRepository;
import me.hanju.branchdown.repository.PointRepository;
import me.hanju.branchdown.repository.StreamRepository;

@DisplayName("StreamService 통합 테스트")
class StreamServiceIntegrationTest extends IntegrationTestBase {

  @Autowired
  private StreamService streamService;

  @Autowired
  private StreamRepository streamRepository;

  @Autowired
  private BranchRepository branchRepository;

  @Autowired
  private PointRepository pointRepository;

  @Nested
  @DisplayName("createStream 메서드는")
  class CreateStreamTests {

    @Test
    @DisplayName("제목이 제공되면 해당 제목으로 스트림을 생성한다")
    void createStreamWithTitle() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("My Chat");

      // When
      StreamDto.Response response = streamService.createStream(request);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isNotNull();
      assertThat(response.title()).isEqualTo("My Chat");
      assertThat(response.createdBy()).isEqualTo(testUserId);

      // DB 검증 - 초기 브랜치와 루트 포인트 자동 생성
      StreamEntity savedStream = streamRepository.findByUuid(response.id()).orElseThrow();
      assertThat(savedStream.getTitle()).isEqualTo("My Chat");
      assertThat(savedStream.getBranches()).hasSize(1);
      assertThat(savedStream.getNextBranchNum()).isEqualTo(1);

      BranchEntity initialBranch = savedStream.getBranches().get(0);
      assertThat(initialBranch.getBranchNum()).isEqualTo(StreamConstants.INITIAL_BRANCH_NUM);
      assertThat(initialBranch.getPath()).isEmpty();
      assertThat(initialBranch.getPoints()).hasSize(1);

      PointEntity rootPoint = initialBranch.getPoints().get(0);
      assertThat(rootPoint.getDepth()).isEqualTo(StreamConstants.ROOT_POINT_DEPTH);
      assertThat(rootPoint.getItemId()).isNull();
      assertThat(rootPoint.getChildBranchNums()).isEmpty();
    }

    @Test
    @DisplayName("제목이 null이면 기본 제목으로 스트림을 생성한다")
    void createStreamWithDefaultTitle() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest();

      // When
      StreamDto.Response response = streamService.createStream(request);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.title()).isEqualTo(StreamConstants.DEFAULT_STREAM_TITLE);
    }

    @Test
    @DisplayName("빈 문자열 제목은 기본 제목으로 변경된다")
    void createStreamWithBlankTitle() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("   ");

      // When
      StreamDto.Response response = streamService.createStream(request);

      // Then
      assertThat(response.title()).isEqualTo(StreamConstants.DEFAULT_STREAM_TITLE);
    }

    @Test
    @DisplayName("초기 브랜치와 루트 포인트를 자동으로 생성한다")
    void createStreamWithInitialBranchAndRootPoint() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("Test");

      // When
      StreamDto.Response response = streamService.createStream(request);

      // Then
      StreamEntity stream = streamRepository.findByUuid(response.id()).orElseThrow();
      assertThat(stream.getBranches()).hasSize(1);
      assertThat(stream.getNextBranchNum()).isEqualTo(1);

      BranchEntity branch = stream.getBranches().get(0);
      assertThat(branch.getPoints()).hasSize(1);

      PointEntity rootPoint = branch.getPoints().get(0);
      assertThat(rootPoint.getDepth()).isEqualTo(StreamConstants.ROOT_POINT_DEPTH);
      assertThat(rootPoint.getItemId()).isNull();
      assertThat(rootPoint.getChildBranchNums()).isEmpty();
    }
  }

  @Nested
  @DisplayName("getStream 메서드는")
  class GetStreamTests {

    @Test
    @DisplayName("UUID로 스트림을 조회한다")
    void getStreamByUuid() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("Test Stream");
      StreamDto.Response created = streamService.createStream(request);

      // When
      StreamDto.Response response = streamService.getStream(created.id());

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(created.id());
      assertThat(response.title()).isEqualTo("Test Stream");
      assertThat(response.createdBy()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("존재하지 않는 UUID로 조회 시 예외를 발생시킨다")
    void getStreamNotFound() {
      // Given
      UUID nonExistentUuid = UUID.randomUUID();

      // When & Then
      assertThatThrownBy(() -> streamService.getStream(nonExistentUuid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Stream not found");
    }
  }

  @Nested
  @DisplayName("getStreams 메서드는")
  class GetStreamsTests {

    @Test
    @DisplayName("검색어가 있으면 제목으로 필터링하여 조회한다")
    void getStreamsWithQuery() {
      // Given
      streamService.createStream(StreamFixture.createRequest("Test Stream 1"));
      streamService.createStream(StreamFixture.createRequest("Test Stream 2"));
      streamService.createStream(StreamFixture.createRequest("Another Stream"));

      Pageable pageable = PageRequest.of(0, 20);

      // When
      Page<StreamDto.ListItem> result = streamService.getStreams("Test", testUserId, pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent()).allMatch(item -> item.getTitle().contains("Test"));
    }

    @Test
    @DisplayName("검색어가 없으면 모든 스트림을 조회한다")
    void getStreamsWithoutQuery() {
      // Given
      streamService.createStream(StreamFixture.createRequest("Stream 1"));
      streamService.createStream(StreamFixture.createRequest("Stream 2"));
      streamService.createStream(StreamFixture.createRequest("Stream 3"));

      Pageable pageable = PageRequest.of(0, 20);

      // When
      Page<StreamDto.ListItem> result = streamService.getStreams(null, testUserId, pageable);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("페이징이 정상 동작한다")
    void getStreamsWithPaging() {
      // Given - 5개 생성
      for (int i = 1; i <= 5; i++) {
        streamService.createStream(StreamFixture.createRequest("Stream " + i));
      }

      Pageable firstPage = PageRequest.of(0, 2);
      Pageable secondPage = PageRequest.of(1, 2);

      // When
      Page<StreamDto.ListItem> first = streamService.getStreams(null, testUserId, firstPage);
      Page<StreamDto.ListItem> second = streamService.getStreams(null, testUserId, secondPage);

      // Then
      assertThat(first.getContent()).hasSize(2);
      assertThat(second.getContent()).hasSize(2);
      assertThat(first.getTotalElements()).isEqualTo(5);
      assertThat(first.getTotalPages()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("updateStream 메서드는")
  class UpdateStreamTests {

    @Test
    @DisplayName("스트림의 제목을 수정한다")
    void updateStreamTitle() {
      // Given
      StreamDto.CreateRequest createRequest = StreamFixture.createRequest("Original Title");
      StreamDto.Response created = streamService.createStream(createRequest);

      StreamDto.UpdateRequest updateRequest = StreamFixture.updateRequest("Updated Title");

      // When
      StreamDto.Response response = streamService.updateStream(created.id(), updateRequest);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(created.id());
      assertThat(response.title()).isEqualTo("Updated Title");
      assertThat(response.updatedAt()).isAfter(response.createdAt());

      // DB 검증
      StreamEntity stream = streamRepository.findByUuid(created.id()).orElseThrow();
      assertThat(stream.getTitle()).isEqualTo("Updated Title");
      assertThat(stream.getUpdatedBy()).isEqualTo(testUserId);
    }

    @Test
    @DisplayName("존재하지 않는 스트림 수정 시 예외를 발생시킨다")
    void updateStreamNotFound() {
      // Given
      UUID nonExistentUuid = UUID.randomUUID();
      StreamDto.UpdateRequest request = StreamFixture.updateRequest("New Title");

      // When & Then
      assertThatThrownBy(() -> streamService.updateStream(nonExistentUuid, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Stream not found");
    }
  }

  @Nested
  @DisplayName("deleteChat 메서드는")
  class DeleteChatTests {

    @Test
    @DisplayName("스트림을 삭제한다")
    void deleteStream() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("To Be Deleted");
      StreamDto.Response created = streamService.createStream(request);

      // When
      streamService.deleteChat(created.id());

      // Then
      assertThat(streamRepository.findByUuid(created.id())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 스트림 삭제 시 예외를 발생시킨다")
    void deleteStreamNotFound() {
      // Given
      UUID nonExistentUuid = UUID.randomUUID();

      // When & Then
      assertThatThrownBy(() -> streamService.deleteChat(nonExistentUuid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Stream not found");
    }

    @Test
    @DisplayName("스트림 삭제 시 연관된 브랜치와 포인트도 함께 삭제된다")
    void deleteStreamCascades() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("Test");
      StreamDto.Response created = streamService.createStream(request);

      StreamEntity stream = streamRepository.findByUuid(created.id()).orElseThrow();
      Long streamId = stream.getId();
      int branchNum = stream.getBranches().get(0).getBranchNum();

      // When
      streamService.deleteChat(created.id());

      // Then - Cascade로 브랜치와 포인트도 삭제됨
      assertThat(streamRepository.findByUuid(created.id())).isEmpty();
      assertThat(branchRepository.findById(new me.hanju.branchdown.entity.id.BranchId(streamId, branchNum)))
          .isEmpty();
    }
  }

  @Nested
  @DisplayName("getStreamPoints 메서드는")
  class GetStreamPointsTests {

    @Test
    @DisplayName("스트림의 최신 브랜치 경로를 따라 포인트 목록을 반환한다")
    void getStreamPoints() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("Test");
      StreamDto.Response stream = streamService.createStream(request);

      // DB에서 루트 포인트 찾기
      StreamEntity streamEntity = streamRepository.findByUuid(stream.id()).orElseThrow();
      BranchEntity initialBranch = streamEntity.getBranches().get(0);
      PointEntity rootPoint = initialBranch.getPoints().get(0);

      // 포인트 추가
      PointEntity point1 = StreamFixture.pointEntity(initialBranch, 1, "item1");
      pointRepository.save(point1);
      rootPoint.addChildBranchNum(initialBranch.getBranchNum());
      pointRepository.save(rootPoint);

      // When
      List<PointDto.Response> result = streamService.getStreamPoints(stream.id());

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSizeGreaterThanOrEqualTo(1);
      assertThat(result.get(0).itemId()).isNull(); // 루트 포인트
      assertThat(result.get(0).branchNum()).isEqualTo(StreamConstants.INITIAL_BRANCH_NUM);
    }

    @Test
    @DisplayName("존재하지 않는 스트림 조회 시 예외를 발생시킨다")
    void getStreamPointsNotFound() {
      // Given
      UUID nonExistentUuid = UUID.randomUUID();

      // When & Then
      assertThatThrownBy(() -> streamService.getStreamPoints(nonExistentUuid))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Stream not found");
    }
  }

  @Nested
  @DisplayName("getBranchMessages 메서드는")
  class GetBranchMessagesTests {

    @Test
    @DisplayName("특정 브랜치의 포인트 목록을 반환한다")
    void getBranchMessages() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("Test");
      StreamDto.Response stream = streamService.createStream(request);

      int branchNum = StreamConstants.INITIAL_BRANCH_NUM;
      int depth = -1; // 처음부터 받기 위해 -1 사용

      // When
      List<PointDto.Response> result = streamService.getBranchMessages(stream.id(), branchNum, depth);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSizeGreaterThanOrEqualTo(1); // 최소 루트 포인트
      assertThat(result.get(0).branchNum()).isEqualTo(branchNum);
    }

    @Test
    @DisplayName("존재하지 않는 브랜치 조회 시 예외를 발생시킨다")
    void getBranchMessagesNotFound() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("Test");
      StreamDto.Response stream = streamService.createStream(request);

      int nonExistentBranchNum = 99;
      int depth = 0;

      // When & Then
      assertThatThrownBy(() -> streamService.getBranchMessages(stream.id(), nonExistentBranchNum, depth))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Branch not found");
    }

    @Test
    @DisplayName("depth 파라미터로 특정 깊이 이후의 포인트만 조회할 수 있다")
    void getBranchMessagesWithDepth() {
      // Given
      StreamDto.CreateRequest request = StreamFixture.createRequest("Test");
      StreamDto.Response stream = streamService.createStream(request);

      StreamEntity streamEntity = streamRepository.findByUuid(stream.id()).orElseThrow();
      BranchEntity branch = streamEntity.getBranches().get(0);

      // depth 1, 2 포인트 추가
      PointEntity point1 = StreamFixture.pointEntity(branch, 1, "item1");
      PointEntity point2 = StreamFixture.pointEntity(branch, 2, "item2");
      pointRepository.save(point1);
      pointRepository.save(point2);

      // When - depth 1 이후만 조회
      List<PointDto.Response> result = streamService.getBranchMessages(
          stream.id(), StreamConstants.INITIAL_BRANCH_NUM, 1);

      // Then - depth > 1인 포인트들만 반환
      assertThat(result).isNotEmpty();
      assertThat(result).allMatch(p -> p.id().equals(point2.getUuid()));
    }
  }
}
