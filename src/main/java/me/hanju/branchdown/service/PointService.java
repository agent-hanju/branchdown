package me.hanju.branchdown.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hanju.branchdown.dto.PointDto;
import me.hanju.branchdown.entity.BranchEntity;
import me.hanju.branchdown.entity.PointEntity;
import me.hanju.branchdown.entity.StreamEntity;
import me.hanju.branchdown.entity.id.BranchId;
import me.hanju.branchdown.repository.BranchRepository;
import me.hanju.branchdown.repository.PointRepository;
import me.hanju.branchdown.util.PathUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

  private final PointRepository pointRepository;
  private final BranchRepository branchRepository;

  /**
   * 지정한 PointEntity 아래에 적절한 브랜칭을 후 PointEntity를 새로 추가한다.
   *
   * @param id     지정할 PointEntity의 id
   * @param itemId 새로 추가할 PointEntity에 들어갈 item의 ID
   * @return
   */
  @Transactional
  public PointDto.Response pointDown(Long id, String itemId) {
    // 1. 기준 포인트 확인
    PointEntity point = pointRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Point not found"));

    // 2. 브랜치 상태 확인
    BranchEntity branch;
    // 2-1. 이어지는 브랜치가 없다면 기존 브랜치 사용
    if (point.getChildBranchNums().isEmpty()) {
      branch = point.getBranch();
    } else {
      // 2-2. 이어지는 브랜치가 있다면 신규 브랜치 생성
      BranchEntity parentBranch = point.getBranch();
      StreamEntity stream = parentBranch.getStream();
      String newPath = PathUtils.appendToPath(parentBranch.getPath(), parentBranch.getBranchNum());

      branch = branchRepository.save(
          BranchEntity.builder()
              .id(new BranchId(stream.getId(), stream.getNextBranchNum()))
              .stream(stream)
              .path(newPath)
              .build());
      stream.addBranch(branch);
    }
    point.addChildBranchNum(branch.getBranchNum());

    // 3. 포인트 추가
    return pointRepository.save(PointEntity.builder().branch(branch).depth(point.getDepth() + 1).itemId(itemId).build())
        .toResponse();
  }
}
