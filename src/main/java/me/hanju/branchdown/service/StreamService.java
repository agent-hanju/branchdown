package me.hanju.branchdown.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hanju.branchdown.constant.StreamConstants;
import me.hanju.branchdown.dto.PointDto;
import me.hanju.branchdown.dto.StreamDto;
import me.hanju.branchdown.entity.BranchEntity;
import me.hanju.branchdown.entity.PointEntity;
import me.hanju.branchdown.entity.StreamEntity;
import me.hanju.branchdown.entity.id.BranchId;
import me.hanju.branchdown.repository.BranchRepository;
import me.hanju.branchdown.repository.PointRepository;
import me.hanju.branchdown.repository.StreamRepository;
import me.hanju.branchdown.util.PathUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamService {

  private final StreamRepository streamRepository;
  private final BranchRepository branchRepository;
  private final PointRepository pointRepository;

  @Transactional
  public StreamDto.Response createStream() {
    // 1. 스트림 생성
    StreamEntity newStream = streamRepository.save(StreamEntity.builder().build());

    // 2. 스트림의 기본 브랜치 생성
    BranchEntity initialBranch = branchRepository.save(BranchEntity.builder()
        .id(new BranchId(newStream.getId(), newStream.getNextBranchNum()))
        .stream(newStream)
        .path("")
        .build());
    newStream.addBranch(initialBranch);

    // 3. 스트림의 루트 포인트 생성(제일 첫 메시지도 브랜칭이 생길 수 있기 때문에 가상의 첫 포인트가 있어야 한다.)
    PointEntity rootPoint = PointEntity.builder()
        .branch(initialBranch)
        .depth(StreamConstants.ROOT_POINT_DEPTH)
        .itemId(null)
        .build();
    pointRepository.save(rootPoint);

    initialBranch.addPoint(rootPoint);

    return newStream.toResponse();
  }

  public StreamDto.Response getStream(Long id) {
    StreamEntity stream = streamRepository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("Stream not found"));
    return stream.toResponse();
  }

  @Transactional
  public void deleteStream(Long id) {
    StreamEntity stream = streamRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Stream not found"));
    streamRepository.delete(stream);
  }

  /**
   * 해당 스트림의 처음부터 가장 최근에 포인트를 추가한 브랜치까지의 스트림에 속하는 포인트 목록을 반환
   */
  public List<PointDto.Response> getStreamPoints(Long id) {
    StreamEntity stream = streamRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Stream not found"));
    BranchEntity latestBranch = branchRepository
        .findLatestBranchInChat(stream)
        .orElseThrow(() -> new IllegalStateException("Latest Branch not found"));

    // path를 int 배열로 변경 후 자기 자신 추가
    int[] branchNums = PathUtils.append(
        PathUtils.parse(latestBranch.getPath()),
        latestBranch.getBranchNum());
    List<PointEntity> messages = pathToPoints(stream.getId(), branchNums, -1);

    return messages.stream().map(PointEntity::toResponse).toList();
  }

  public List<PointDto.Response> getBranchMessages(Long id, int branchNum, int depth) {
    StreamEntity stream = streamRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("Stream not found"));

    BranchEntity branch = branchRepository
        .findById(new BranchId(stream.getId(), branchNum))
        .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

    String basePath = PathUtils.append(branch.getPath(), branchNum);
    int[] branchNums = PathUtils.parse(basePath);

    List<PointEntity> points = pathToPoints(stream.getId(), branchNums, depth);
    return points.stream().map(PointEntity::toResponse).toList();
  }

  private List<PointEntity> pathToPoints(Long streamId, int[] branchNums, int depth) {
    List<PointEntity> messages = pointRepository.findAllUsingPath(
        streamId, Arrays.stream(branchNums).boxed().toList(), depth);

    // 위 쿼리는 각 depth 별 최대 branchNum인 message들을 가져오므로 branchNum 변곡점에서 절삭
    int i = 0;
    int maxBranchNum = 0;
    List<PointEntity> clippedMessages = new ArrayList<>();
    while (i < messages.size() && maxBranchNum <= messages.get(i).getBranchNum()) {
      clippedMessages.add(messages.get(i));
      maxBranchNum = messages.get(i).getBranchNum();
      i += 1;
    }
    return clippedMessages;
  }
}
