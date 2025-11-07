package me.hanju.branchdown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BranchdownApplication {
  public static void main(String[] args) {
    SpringApplication.run(BranchdownApplication.class, args);
  }
}
