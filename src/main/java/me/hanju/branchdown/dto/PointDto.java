package me.hanju.branchdown.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public class PointDto {
  private PointDto() {
  }

  public static record Response(
      UUID id,
      Integer branchNum,
      String itemId,
      List<Integer> childBranchNums,
      String createdBy,
      Instant createdAt) {
  }

  public static record CreateRequest(
      @NotNull(message = "Stream ID is required") UUID streamId) {
  }

  public static record DownRequest(
      @NotNull(message = "Item ID is required") String itemId) {
  }
}
