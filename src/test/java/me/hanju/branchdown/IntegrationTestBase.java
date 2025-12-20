package me.hanju.branchdown;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import me.hanju.branchdown.config.TestcontainersConfig;

/**
 * 통합 테스트를 위한 베이스 클래스
 *
 * <p>모든 통합 테스트는 이 클래스를 상속받아 공통 설정을 사용합니다.</p>
 *
 * <h3>제공 기능</h3>
 * <ul>
 *   <li>Testcontainers MariaDB 자동 설정 (TestcontainersConfig)</li>
 *   <li>테스트용 프로파일 활성화 (test)</li>
 *   <li>트랜잭션 롤백으로 테스트 격리</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * @DisplayName("MyService 통합 테스트")
 * class MyServiceIntegrationTest extends IntegrationTestBase {
 *     @Autowired
 *     private MyService myService;
 *
 *     @Test
 *     void test() {
 *         // 테스트 실행
 *     }
 * }
 * }</pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfig.class)
@Transactional
@ActiveProfiles("test")
public abstract class IntegrationTestBase {
}
