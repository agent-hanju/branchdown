package me.hanju.branchdown.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class PointDto {
  private PointDto() {
  }

  public static record Response(
      Long id,
      Integer branchNum,
      Integer depth,
      String itemId,
      List<Integer> childBranchNums,
      Instant createdAt) {
  }

  public static record DownRequest(
      @NotNull(message = "Item ID is required") String itemId) {
  }
}
