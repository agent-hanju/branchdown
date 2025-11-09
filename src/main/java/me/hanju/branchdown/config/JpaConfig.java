package me.hanju.branchdown.config;

import java.util.Optional;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import me.hanju.auth.validator.domain.Account;

/**
 * JPA 설정
 *
 * <p>
 * JPA Auditing을 통해 createdBy, lastModifiedBy 필드를 자동으로 채움
 * (JWT 토큰에서 현재 사용자의 publicId를 추출하여 사용)
 * </p>
 */
@Configuration
@EntityScan(basePackages = {
    "me.hanju.branchdown.entity"
})
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = {
    "me.hanju.branchdown.repository"
})
@EnableTransactionManagement
public class JpaConfig {

  /**
   * 현재 사용자의 publicId를 제공하는 AuditorAware Bean
   *
   * <p>
   * Spring Security Context에서 인증된 사용자의 publicId를 추출
   * </p>
   *
   * @return AuditorAware<String> 현재 사용자의 publicId를 반환
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication == null ||
          !authentication.isAuthenticated() ||
          "anonymousUser".equals(authentication.getPrincipal())) {
        return Optional.of("system");
      }

      Object principal = authentication.getPrincipal();

      if (principal instanceof Account account) {
        return Optional.of(account.getPublicId());
      }

      return Optional.of("system");
    };
  }
}
