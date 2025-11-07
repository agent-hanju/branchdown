package me.hanju.branchdown.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StreamDto {
  private StreamDto() {
  }

  public static interface ListItem {
    UUID getId();

    String getTitle();

    Instant getUpdatedAt();
  }

  public static record Response(
      UUID id,
      String title,
      String createdBy,
      Instant createdAt,
      String updatedBy,
      Instant updatedAt) {
  }

  public static record CreateRequest(
      @Size(max = 255, message = "Title must not exceed 255 characters") String title) {
  }

  public static record UpdateRequest(
      @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must not exceed 255 characters") String title) {
  }
}
