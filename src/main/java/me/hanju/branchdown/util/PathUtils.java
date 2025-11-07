package me.hanju.branchdown.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Path 파싱을 위한 유틸리티 클래스
 */
public class PathUtils {

  /**
   * 쉼표로 구분된 path 문자열을 Integer List로 변환합니다.
   * 빈 문자열이나 공백은 무시됩니다.
   *
   * @param path 쉼표로 구분된 path 문자열 (예: "0,1,2" 또는 "")
   * @return Integer List
   * @throws IllegalArgumentException path에 숫자가 아닌 값이 포함된 경우
   */
  public static List<Integer> parsePath(String path) {
    List<Integer> branchNums = new ArrayList<>();

    if (path == null || path.isBlank()) {
      return branchNums;
    }

    String[] parts = path.split(",");
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        try {
          branchNums.add(Integer.valueOf(trimmed));
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Invalid path format: " + path, e);
        }
      }
    }

    return branchNums;
  }

  /**
   * Branch path에 branchNum을 추가합니다.
   *
   * @param basePath 기존 path (예: "0,1" 또는 "")
   * @param branchNum 추가할 branchNum
   * @return 새로운 path 문자열
   */
  public static String appendToPath(String basePath, int branchNum) {
    if (basePath == null || basePath.isEmpty()) {
      return String.valueOf(branchNum);
    }
    return basePath + "," + branchNum;
  }
}
