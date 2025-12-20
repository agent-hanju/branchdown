package me.hanju.branchdown.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    @DisplayName("스트림을 생성한다")
    void createStream() {
      // When
      StreamDto.Response response = streamService.createStream();

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isNotNull();

      // DB 검증 - 초기 브랜치와 루트 포인트 자동 생성
      StreamEntity savedStream = streamRepository.findById(response.id()).orElseThrow();
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
    @DisplayName("초기 브랜치와 루트 포인트를 자동으로 생성한다")
    void createStreamWithInitialBranchAndRootPoint() {
      // When
      StreamDto.Response response = streamService.createStream();

      // Then
      StreamEntity stream = streamRepository.findById(response.id()).orElseThrow();
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
    @DisplayName("ID로 스트림을 조회한다")
    void getStreamById() {
      // Given
      StreamDto.Response created = streamService.createStream();

      // When
      StreamDto.Response response = streamService.getStream(created.id());

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(created.id());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 예외를 발생시킨다")
    void getStreamNotFound() {
      // Given
      Long nonExistentId = 999999L;

      // When & Then
      assertThatThrownBy(() -> streamService.getStream(nonExistentId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Stream not found");
    }
  }

  @Nested
  @DisplayName("deleteStream 메서드는")
  class DeleteStreamTests {

    @Test
    @DisplayName("스트림을 삭제한다")
    void deleteStream() {
      // Given
      StreamDto.Response created = streamService.createStream();

      // When
      streamService.deleteStream(created.id());

      // Then
      assertThat(streamRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 스트림 삭제 시 예외를 발생시킨다")
    void deleteStreamNotFound() {
      // Given
      Long nonExistentId = 999999L;

      // When & Then
      assertThatThrownBy(() -> streamService.deleteStream(nonExistentId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Stream not found");
    }

    @Test
    @DisplayName("스트림 삭제 시 연관된 브랜치와 포인트도 함께 삭제된다")
    void deleteStreamCascades() {
      // Given
      StreamDto.Response created = streamService.createStream();

      StreamEntity stream = streamRepository.findById(created.id()).orElseThrow();
      Long streamId = stream.getId();
      int branchNum = stream.getBranches().get(0).getBranchNum();

      // When
      streamService.deleteStream(created.id());

      // Then - Cascade로 브랜치와 포인트도 삭제됨
      assertThat(streamRepository.findById(created.id())).isEmpty();
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
      StreamDto.Response stream = streamService.createStream();

      // DB에서 루트 포인트 찾기
      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
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
      Long nonExistentId = 999999L;

      // When & Then
      assertThatThrownBy(() -> streamService.getStreamPoints(nonExistentId))
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
      StreamDto.Response stream = streamService.createStream();

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
      StreamDto.Response stream = streamService.createStream();

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
      StreamDto.Response stream = streamService.createStream();

      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
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
      assertThat(result).allMatch(p -> p.id().equals(point2.getId()));
    }
  }
}
