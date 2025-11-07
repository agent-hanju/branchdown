package me.hanju.branchdown;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import me.hanju.auth.validator.domain.Account;
import me.hanju.branchdown.config.TestConfig;

/**
 * 통합 테스트를 위한 베이스 클래스
 *
 * <p>모든 통합 테스트는 이 클래스를 상속받아 공통 설정을 사용합니다.</p>
 *
 * <h3>제공 기능</h3>
 * <ul>
 *   <li>H2 인메모리 데이터베이스 자동 설정</li>
 *   <li>테스트용 프로파일 활성화 (test)</li>
 *   <li>트랜잭션 롤백으로 테스트 격리</li>
 *   <li>기본 테스트 사용자 인증 설정</li>
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
 *         // testUserId로 인증된 상태에서 테스트 실행
 *     }
 * }
 * }</pre>
 */
@SpringBootTest(
    properties = {
        "hanju.jwt-validator.enabled=false"
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(TestConfig.class)
@Transactional
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  /** 테스트용 기본 사용자 ID */
  protected String testUserId = "test-user";

  /**
   * 각 테스트 실행 전 인증 컨텍스트를 설정합니다.
   *
   * <p>테스트 클래스에서 다른 사용자로 테스트하려면 이 메서드를 오버라이드하거나
   * {@link #setAuthentication(String)} 메서드를 사용하세요.</p>
   */
  @BeforeEach
  void setUpAuthentication() {
    setAuthentication(testUserId);
  }

  /**
   * 지정된 사용자 ID로 Spring Security 인증 컨텍스트를 설정합니다.
   *
   * @param userId 인증할 사용자 ID
   */
  protected void setAuthentication(String userId) {
    Account account = new Account();
    account.setPublicId(userId);
    // authorities를 제공하는 생성자를 사용하면 자동으로 authenticated=true가 됨
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(account, null, java.util.Collections.emptyList());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  /**
   * 인증 컨텍스트를 초기화합니다.
   */
  protected void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }
}
