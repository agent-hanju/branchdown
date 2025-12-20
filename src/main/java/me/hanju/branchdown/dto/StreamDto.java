package me.hanju.branchdown.dto;

import java.time.Instant;

public class StreamDto {
  private StreamDto() {
  }

  public static record Response(
      Long id,
      Instant createdAt) {
  }
}
