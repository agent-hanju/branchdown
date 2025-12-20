package me.hanju.branchdown.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import me.hanju.branchdown.IntegrationTestBase;
import me.hanju.branchdown.dto.PointDto;
import me.hanju.branchdown.dto.StreamDto;
import me.hanju.branchdown.entity.BranchEntity;
import me.hanju.branchdown.entity.PointEntity;
import me.hanju.branchdown.entity.StreamEntity;
import me.hanju.branchdown.repository.BranchRepository;
import me.hanju.branchdown.repository.PointRepository;
import me.hanju.branchdown.repository.StreamRepository;

@DisplayName("PointService 통합 테스트")
class PointServiceIntegrationTest extends IntegrationTestBase {

  @Autowired
  private PointService pointService;

  @Autowired
  private StreamService streamService;

  @Autowired
  private StreamRepository streamRepository;

  @Autowired
  private BranchRepository branchRepository;

  @Autowired
  private PointRepository pointRepository;

  @Nested
  @DisplayName("pointDown 메서드는")
  class PointDownTests {

    @Test
    @DisplayName("자식 브랜치가 없으면 기존 브랜치에 새 포인트를 추가한다")
    void pointDownWithoutChildBranches() {
      // Given - 새 스트림 생성
      StreamDto.Response stream = streamService.createStream();

      // 루트 포인트 찾기
      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
      BranchEntity initialBranch = streamEntity.getBranches().get(0);
      PointEntity rootPoint = initialBranch.getPoints().get(0);
      Long rootPointId = rootPoint.getId();

      // When - 루트 포인트 아래에 새 포인트 추가
      PointDto.Response response = pointService.pointDown(rootPointId, "item123");

      // Then
      assertThat(response).isNotNull();
      assertThat(response.itemId()).isEqualTo("item123");
      assertThat(response.branchNum()).isEqualTo(0); // 같은 브랜치

      // DB 검증
      PointEntity savedPoint = pointRepository.findById(response.id()).orElseThrow();
      assertThat(savedPoint.getDepth()).isEqualTo(1); // rootPoint(0) + 1
      assertThat(savedPoint.getBranch().getBranchNum()).isEqualTo(0);

      // 루트 포인트의 childBranchNums 확인
      PointEntity updatedRootPoint = pointRepository.findById(rootPointId).orElseThrow();
      assertThat(updatedRootPoint.getChildBranchNums()).contains(0);
    }

    @Test
    @DisplayName("자식 브랜치가 있으면 새 브랜치를 생성하고 포인트를 추가한다")
    void pointDownWithChildBranches() {
      // Given - 새 스트림 생성
      StreamDto.Response stream = streamService.createStream();

      // 루트 포인트 찾기
      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
      BranchEntity initialBranch = streamEntity.getBranches().get(0);
      PointEntity rootPoint = initialBranch.getPoints().get(0);
      Long rootPointId = rootPoint.getId();

      // 첫 번째 포인트 추가 (브랜치 0에)
      PointDto.Response firstPoint = pointService.pointDown(rootPointId, "item1");
      assertThat(firstPoint.branchNum()).isEqualTo(0);

      // When - 같은 루트 포인트에서 다시 추가 (브랜칭 발생)
      PointDto.Response secondPoint = pointService.pointDown(rootPointId, "item2-alternative");

      // Then
      assertThat(secondPoint).isNotNull();
      assertThat(secondPoint.itemId()).isEqualTo("item2-alternative");
      assertThat(secondPoint.branchNum()).isEqualTo(1); // 새 브랜치

      // DB 검증 - 새 브랜치가 생성되었는지 확인
      StreamEntity updatedStream = streamRepository.findById(stream.id()).orElseThrow();
      assertThat(updatedStream.getBranches()).hasSize(2); // 초기 브랜치 + 새 브랜치
      assertThat(updatedStream.getNextBranchNum()).isEqualTo(2);

      // 새 브랜치 확인
      BranchEntity newBranch = updatedStream.getBranches().stream()
          .filter(b -> b.getBranchNum() == 1)
          .findFirst()
          .orElseThrow();

      assertThat(newBranch.getPath()).isEqualTo("0"); // 초기 브랜치(0)에서 분기

      // 루트 포인트의 childBranchNums 확인
      PointEntity updatedRootPoint = pointRepository.findById(rootPointId).orElseThrow();
      assertThat(updatedRootPoint.getChildBranchNums()).hasSize(2).contains(0, 1);
    }

    @Test
    @DisplayName("깊이 2에서 브랜칭이 발생할 때 올바른 경로를 생성한다")
    void pointDownAtDepth2WithBranching() {
      // Given - 스트림 생성
      StreamDto.Response stream = streamService.createStream();

      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
      BranchEntity initialBranch = streamEntity.getBranches().get(0);
      PointEntity rootPoint = initialBranch.getPoints().get(0);

      // depth 1 포인트 추가
      PointDto.Response depth1Point = pointService.pointDown(rootPoint.getId(), "msg1");

      // depth 2 포인트 추가 (첫 번째)
      PointDto.Response depth2FirstPoint = pointService.pointDown(depth1Point.id(), "msg2-first");
      assertThat(depth2FirstPoint.branchNum()).isEqualTo(0); // 같은 브랜치

      // When - depth 1 포인트에서 다시 포인트 추가 (브랜칭 발생)
      PointDto.Response depth2SecondPoint = pointService.pointDown(depth1Point.id(), "msg2-alternative");

      // Then
      assertThat(depth2SecondPoint).isNotNull();
      assertThat(depth2SecondPoint.itemId()).isEqualTo("msg2-alternative");
      assertThat(depth2SecondPoint.branchNum()).isEqualTo(1); // 새 브랜치

      // 새 브랜치의 경로 확인
      StreamEntity updatedStream = streamRepository.findById(stream.id()).orElseThrow();
      BranchEntity newBranch = updatedStream.getBranches().stream()
          .filter(b -> b.getBranchNum() == 1)
          .findFirst()
          .orElseThrow();

      assertThat(newBranch.getPath()).isEqualTo("0"); // 브랜치 0에서 분기

      // depth 2의 새 포인트 확인
      PointEntity savedPoint = pointRepository.findById(depth2SecondPoint.id()).orElseThrow();
      assertThat(savedPoint.getDepth()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 포인트에 대해 예외를 발생시킨다")
    void pointDownPointNotFound() {
      // Given
      Long nonExistentId = 999999L;
      String itemId = "item999";

      // When & Then
      assertThatThrownBy(() -> pointService.pointDown(nonExistentId, itemId))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Point not found");
    }
  }

  @Nested
  @DisplayName("브랜칭 시나리오 통합 테스트")
  class BranchingScenarioTests {

    @Test
    @DisplayName("선형 대화 흐름: 루트 -> msg1 -> msg2 -> msg3")
    void linearConversationFlow() {
      // Given - 스트림 생성
      StreamDto.Response stream = streamService.createStream();

      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
      PointEntity rootPoint = streamEntity.getBranches().get(0).getPoints().get(0);

      // When - 선형으로 메시지 추가
      PointDto.Response msg1 = pointService.pointDown(rootPoint.getId(), "msg1");
      PointDto.Response msg2 = pointService.pointDown(msg1.id(), "msg2");
      PointDto.Response msg3 = pointService.pointDown(msg2.id(), "msg3");

      // Then - 모두 같은 브랜치(0)에 추가되어야 함
      assertThat(msg1.branchNum()).isEqualTo(0);
      assertThat(msg2.branchNum()).isEqualTo(0);
      assertThat(msg3.branchNum()).isEqualTo(0);

      // depth 확인
      PointEntity msg1Entity = pointRepository.findById(msg1.id()).orElseThrow();
      PointEntity msg2Entity = pointRepository.findById(msg2.id()).orElseThrow();
      PointEntity msg3Entity = pointRepository.findById(msg3.id()).orElseThrow();

      assertThat(msg1Entity.getDepth()).isEqualTo(1);
      assertThat(msg2Entity.getDepth()).isEqualTo(2);
      assertThat(msg3Entity.getDepth()).isEqualTo(3);

      // childBranchNums 확인
      PointEntity updatedRootPoint = pointRepository.findById(rootPoint.getId()).orElseThrow();
      assertThat(updatedRootPoint.getChildBranchNums()).containsExactly(0);

      PointEntity updatedMsg1 = pointRepository.findById(msg1.id()).orElseThrow();
      assertThat(updatedMsg1.getChildBranchNums()).containsExactly(0);

      // 브랜치 개수 확인
      StreamEntity updatedStream = streamRepository.findById(stream.id()).orElseThrow();
      assertThat(updatedStream.getBranches()).hasSize(1); // 브랜치 분기 없음
    }

    @Test
    @DisplayName("분기 대화 흐름: 루트에서 여러 방향으로 분기")
    void branchedConversationFlow() {
      // Given - 스트림 생성
      StreamDto.Response stream = streamService.createStream();

      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
      PointEntity rootPoint = streamEntity.getBranches().get(0).getPoints().get(0);

      // When - 루트에서 3개의 다른 방향으로 메시지 추가
      PointDto.Response branch0Msg = pointService.pointDown(rootPoint.getId(), "msg-branch0");
      PointDto.Response branch1Msg = pointService.pointDown(rootPoint.getId(), "msg-branch1");
      PointDto.Response branch2Msg = pointService.pointDown(rootPoint.getId(), "msg-branch2");

      // Then - 첫 번째는 기존 브랜치, 나머지는 새 브랜치
      assertThat(branch0Msg.branchNum()).isEqualTo(0);
      assertThat(branch1Msg.branchNum()).isEqualTo(1);
      assertThat(branch2Msg.branchNum()).isEqualTo(2);

      // 루트 포인트의 childBranchNums 확인
      PointEntity updatedRootPoint = pointRepository.findById(rootPoint.getId()).orElseThrow();
      assertThat(updatedRootPoint.getChildBranchNums()).containsExactlyInAnyOrder(0, 1, 2);

      // 브랜치 개수 확인
      StreamEntity updatedStream = streamRepository.findById(stream.id()).orElseThrow();
      assertThat(updatedStream.getBranches()).hasSize(3); // 초기 + 2개 분기
      assertThat(updatedStream.getNextBranchNum()).isEqualTo(3);

      // 각 브랜치의 경로 확인
      BranchEntity branch1 = updatedStream.getBranches().stream()
          .filter(b -> b.getBranchNum() == 1)
          .findFirst()
          .orElseThrow();
      assertThat(branch1.getPath()).isEqualTo("0");

      BranchEntity branch2 = updatedStream.getBranches().stream()
          .filter(b -> b.getBranchNum() == 2)
          .findFirst()
          .orElseThrow();
      assertThat(branch2.getPath()).isEqualTo("0");
    }

    @Test
    @DisplayName("복잡한 분기: depth 2에서 여러 브랜치로 분기")
    void complexBranchingScenario() {
      // Given
      StreamDto.Response stream = streamService.createStream();

      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
      PointEntity rootPoint = streamEntity.getBranches().get(0).getPoints().get(0);

      // When
      // 선형: root -> msg1 -> msg2
      PointDto.Response msg1 = pointService.pointDown(rootPoint.getId(), "msg1");
      PointDto.Response msg2 = pointService.pointDown(msg1.id(), "msg2");

      // msg1에서 분기: msg1 -> msg2-alt1, msg2-alt2
      PointDto.Response msg2Alt1 = pointService.pointDown(msg1.id(), "msg2-alt1");
      PointDto.Response msg2Alt2 = pointService.pointDown(msg1.id(), "msg2-alt2");

      // Then
      assertThat(msg2.branchNum()).isEqualTo(0); // 기존 브랜치
      assertThat(msg2Alt1.branchNum()).isEqualTo(1); // 새 브랜치
      assertThat(msg2Alt2.branchNum()).isEqualTo(2); // 또 다른 새 브랜치

      // msg1의 childBranchNums 확인
      PointEntity updatedMsg1 = pointRepository.findById(msg1.id()).orElseThrow();
      assertThat(updatedMsg1.getChildBranchNums()).containsExactlyInAnyOrder(0, 1, 2);

      // depth 확인
      PointEntity msg2Entity = pointRepository.findById(msg2.id()).orElseThrow();
      PointEntity msg2Alt1Entity = pointRepository.findById(msg2Alt1.id()).orElseThrow();
      PointEntity msg2Alt2Entity = pointRepository.findById(msg2Alt2.id()).orElseThrow();

      assertThat(msg2Entity.getDepth()).isEqualTo(2);
      assertThat(msg2Alt1Entity.getDepth()).isEqualTo(2);
      assertThat(msg2Alt2Entity.getDepth()).isEqualTo(2);

      // 브랜치 개수 확인
      StreamEntity updatedStream = streamRepository.findById(stream.id()).orElseThrow();
      assertThat(updatedStream.getBranches()).hasSize(3); // 초기 + 2개 분기
    }

    @Test
    @DisplayName("다단계 브랜칭: 브랜치 내에서 또 다시 브랜칭")
    void multiLevelBranching() {
      // Given
      StreamDto.Response stream = streamService.createStream();

      StreamEntity streamEntity = streamRepository.findById(stream.id()).orElseThrow();
      PointEntity rootPoint = streamEntity.getBranches().get(0).getPoints().get(0);

      // When
      // 1단계: root -> A
      PointDto.Response pointA = pointService.pointDown(rootPoint.getId(), "A");

      // 2단계: root -> B (branch 1 생성)
      PointDto.Response pointB = pointService.pointDown(rootPoint.getId(), "B");

      // 3단계: A -> A1, A2 (branch 2 생성)
      PointDto.Response pointA1 = pointService.pointDown(pointA.id(), "A1");
      PointDto.Response pointA2 = pointService.pointDown(pointA.id(), "A2");

      // 4단계: B -> B1 (branch 1 계속)
      PointDto.Response pointB1 = pointService.pointDown(pointB.id(), "B1");

      // Then
      // Branch 구조 확인
      assertThat(pointA.branchNum()).isEqualTo(0);
      assertThat(pointB.branchNum()).isEqualTo(1);
      assertThat(pointA1.branchNum()).isEqualTo(0); // A의 첫 번째 child
      assertThat(pointA2.branchNum()).isEqualTo(2); // A의 두 번째 child (새 branch)
      assertThat(pointB1.branchNum()).isEqualTo(1); // B 계속

      // childBranchNums 확인
      PointEntity updatedRoot = pointRepository.findById(rootPoint.getId()).orElseThrow();
      assertThat(updatedRoot.getChildBranchNums()).containsExactlyInAnyOrder(0, 1);

      PointEntity updatedA = pointRepository.findById(pointA.id()).orElseThrow();
      assertThat(updatedA.getChildBranchNums()).containsExactlyInAnyOrder(0, 2);

      PointEntity updatedB = pointRepository.findById(pointB.id()).orElseThrow();
      assertThat(updatedB.getChildBranchNums()).containsExactly(1);

      // 총 브랜치 개수
      StreamEntity updatedStream = streamRepository.findById(stream.id()).orElseThrow();
      assertThat(updatedStream.getBranches()).hasSize(3); // branch 0, 1, 2

      // Branch path 확인
      BranchEntity branch2 = updatedStream.getBranches().stream()
          .filter(b -> b.getBranchNum() == 2)
          .findFirst()
          .orElseThrow();
      assertThat(branch2.getPath()).isEqualTo("0"); // branch 0에서 분기
    }
  }
}
