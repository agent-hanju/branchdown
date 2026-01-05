package me.hanju.branchdown.dto;

import java.time.Instant;
import java.util.List;

/** Branchdown Point DTO */
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
      String itemId) {
  }
}
