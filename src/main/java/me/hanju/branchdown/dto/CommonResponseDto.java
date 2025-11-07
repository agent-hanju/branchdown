package me.hanju.branchdown.dto;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommonResponseDto<T> {
  private boolean success;
  @Nullable
  private T data;
  @Nullable
  private String message;

  public static <T> CommonResponseDto<T> success(T data) {
    return CommonResponseDto.<T>builder()
        .success(true)
        .data(data)
        .build();
  }

  public static <T> CommonResponseDto<T> error(String message) {
    return CommonResponseDto.<T>builder()
        .success(false)
        .message(message)
        .build();
  }

  public static <T> CommonResponseDto<T> error(String message, T data) {
    return CommonResponseDto.<T>builder()
        .success(false)
        .data(data)
        .message(message)
        .build();
  }
}
