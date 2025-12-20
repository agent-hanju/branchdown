package me.hanju.branchdown.fixture;

import java.util.ArrayList;

import me.hanju.branchdown.entity.BranchEntity;
import me.hanju.branchdown.entity.PointEntity;
import me.hanju.branchdown.entity.StreamEntity;
import me.hanju.branchdown.entity.id.BranchId;

/**
 * 테스트용 Stream 관련 Fixture
 *
 * <p>테스트에서 사용할 Stream, Branch, Point 엔티티를 쉽게 생성할 수 있습니다.</p>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // Entity 생성
 * StreamEntity stream = StreamFixture.streamEntity();
 * BranchEntity branch = StreamFixture.branchEntity(stream, 0, "");
 * PointEntity point = StreamFixture.pointEntity(branch, 0, "item-1");
 * }</pre>
 */
public class StreamFixture {

  private StreamFixture() {
    // 유틸리티 클래스는 인스턴스화 방지
  }

  /**
   * StreamEntity를 생성합니다.
   *
   * @return StreamEntity
   */
  public static StreamEntity streamEntity() {
    return StreamEntity.builder()
        .branches(new ArrayList<>())
        .nextBranchNum(0)
        .build();
  }

  /**
   * BranchEntity를 생성합니다.
   *
   * @param stream 소속 스트림
   * @param branchNum 브랜치 번호
   * @param path 브랜치 경로 (예: "0,1,2" 또는 "")
   * @return BranchEntity
   */
  public static BranchEntity branchEntity(StreamEntity stream, int branchNum, String path) {
    return BranchEntity.builder()
        .id(new BranchId(stream.getId(), branchNum))
        .stream(stream)
        .path(path)
        .points(new ArrayList<>())
        .build();
  }

  /**
   * PointEntity를 생성합니다.
   *
   * @param branch 소속 브랜치
   * @param depth 포인트 깊이
   * @param itemId 아이템 ID (null 가능)
   * @return PointEntity
   */
  public static PointEntity pointEntity(BranchEntity branch, int depth, String itemId) {
    return PointEntity.builder()
        .branch(branch)
        .depth(depth)
        .itemId(itemId)
        .childBranchNums(new ArrayList<>())
        .build();
  }

  /**
   * 루트 PointEntity를 생성합니다.
   *
   * @param branch 소속 브랜치
   * @return PointEntity (depth=0, itemId=null)
   */
  public static PointEntity rootPointEntity(BranchEntity branch) {
    return pointEntity(branch, 0, null);
  }
}
