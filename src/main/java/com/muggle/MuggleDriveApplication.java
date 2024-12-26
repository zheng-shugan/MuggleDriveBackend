package com.muggle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.muggle")
@EnableTransactionManagement
public class MuggleDriveApplication {
  public static void main(String[] args) {
    SpringApplication.run(MuggleDriveApplication.class);
  }
}
