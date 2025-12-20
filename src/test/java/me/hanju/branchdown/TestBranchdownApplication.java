package me.hanju.branchdown;

import org.springframework.boot.SpringApplication;

import me.hanju.branchdown.config.TestcontainersConfig;

/**
 * bootTestRun 실행을 위한 테스트 애플리케이션
 *
 * <p>Testcontainers를 사용하여 MariaDB 컨테이너와 함께 애플리케이션을 실행합니다.</p>
 *
 * <pre>
 * ./gradlew bootTestRun
 * </pre>
 */
public class TestBranchdownApplication {

  public static void main(String[] args) {
    SpringApplication.from(BranchdownApplication::main)
        .with(TestcontainersConfig.class)
        .run(args);
  }
}
