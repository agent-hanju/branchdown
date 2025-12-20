package me.hanju.branchdown.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.hanju.branchdown.dto.CommonResponseDto;
import me.hanju.branchdown.dto.PointDto;
import me.hanju.branchdown.service.PointService;

@Tag(name = "Point", description = "포인트 관리 API")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

  private final PointService pointService;

  @Operation(summary = "포인트 추가", description = "지정한 포인트 아래에 새로운 포인트를 추가합니다 (브랜칭 포함)")
  @PostMapping("/{id}/down")
  public ResponseEntity<CommonResponseDto<PointDto.Response>> pointDown(
      @PathVariable Long id,
      @Valid @RequestBody PointDto.DownRequest request) {
    PointDto.Response response = pointService.pointDown(id, request.itemId());
    return ResponseEntity.ok(CommonResponseDto.success(response));
  }
}
