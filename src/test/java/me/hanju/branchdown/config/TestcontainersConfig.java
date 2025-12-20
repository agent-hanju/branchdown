package me.hanju.branchdown.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;

/**
 * bootTestRun을 위한 Testcontainers 설정
 *
 * <p>이 설정은 bootTestRun 실행 시 MariaDB 컨테이너를 자동으로 시작합니다.</p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

  @Bean
  @ServiceConnection
  MariaDBContainer<?> mariaDBContainer() {
    return new MariaDBContainer<>("mariadb:10.11")
        .withDatabaseName("branchdown")
        .withUsername("test")
        .withPassword("test");
  }
}
