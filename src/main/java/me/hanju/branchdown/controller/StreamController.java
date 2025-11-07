package me.hanju.branchdown.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.hanju.branchdown.dto.CommonResponseDto;
import me.hanju.branchdown.dto.PointDto;
import me.hanju.branchdown.dto.StreamDto;
import me.hanju.branchdown.service.StreamService;

@Tag(name = "Stream", description = "스트림 관리 API")
@RestController
@RequestMapping("/api/streams")
@RequiredArgsConstructor
public class StreamController {

  private final StreamService streamService;

  @Operation(summary = "스트림 생성", description = "새로운 스트림을 생성합니다")
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommonResponseDto<StreamDto.Response>> createStream(
      @Valid @RequestBody StreamDto.CreateRequest request) {
    StreamDto.Response response = streamService.createStream(request);
    return ResponseEntity.ok(CommonResponseDto.success(response));
  }

  @Operation(summary = "스트림 조회", description = "스트림 UUID로 스트림을 조회합니다")
  @GetMapping("/{uuid}")
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommonResponseDto<StreamDto.Response>> getStream(
      @PathVariable UUID uuid) {
    StreamDto.Response response = streamService.getStream(uuid);
    return ResponseEntity.ok(CommonResponseDto.success(response));
  }

  @Operation(summary = "스트림 목록 조회", description = "사용자의 스트림 목록을 조회합니다")
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommonResponseDto<Page<StreamDto.ListItem>>> getStreams(
      @RequestParam(name = "q", required = false) String query,
      @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
      @AuthenticationPrincipal(expression = "publicId") String publicId) {
    Page<StreamDto.ListItem> response = streamService.getStreams(query, publicId, pageable);
    return ResponseEntity.ok(CommonResponseDto.success(response));
  }

  @Operation(summary = "스트림 수정", description = "스트림 정보를 수정합니다")
  @PatchMapping("/{uuid}")
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommonResponseDto<StreamDto.Response>> updateStream(
      @PathVariable UUID uuid,
      @Valid @RequestBody StreamDto.UpdateRequest request) {
    StreamDto.Response response = streamService.updateStream(uuid, request);
    return ResponseEntity.ok(CommonResponseDto.success(response));
  }

  @Operation(summary = "스트림 삭제", description = "스트림을 삭제합니다")
  @DeleteMapping("/{uuid}")
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommonResponseDto<Void>> deleteStream(
      @PathVariable UUID uuid) {
    streamService.deleteChat(uuid);
    return ResponseEntity.ok(CommonResponseDto.success(null));
  }

  @Operation(summary = "스트림 포인트 목록 조회", description = "스트림의 처음부터 가장 최근 브랜치까지의 포인트 목록을 조회합니다")
  @GetMapping("/{uuid}/points")
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommonResponseDto<List<PointDto.Response>>> getStreamPoints(
      @PathVariable UUID uuid) {
    List<PointDto.Response> response = streamService.getStreamPoints(uuid);
    return ResponseEntity.ok(CommonResponseDto.success(response));
  }

  @Operation(summary = "브랜치 포인트 목록 조회", description = "특정 브랜치의 포인트 목록을 조회합니다")
  @GetMapping("/{uuid}/branches/{branchNum}/points")
  @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
  public ResponseEntity<CommonResponseDto<List<PointDto.Response>>> getBranchMessages(
      @PathVariable UUID uuid,
      @PathVariable int branchNum,
      @RequestParam(defaultValue = "0") int depth) {
    List<PointDto.Response> response = streamService.getBranchMessages(uuid, branchNum, depth);
    return ResponseEntity.ok(CommonResponseDto.success(response));
  }
}
