package me.hanju.branchdown.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import me.hanju.auth.validator.autoconfigure.JwtValidatorConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtValidatorConfigurer jwtValidatorConfigurer;

  @Bean
  @Profile("!prod")
  public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())
        .build();
  }

  @Bean
  @Profile("prod")
  public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
    jwtValidatorConfigurer.configure(http);
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .anyRequest().authenticated())
        .build();
  }
}
