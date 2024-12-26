package com.muggle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@MapperScan(value = "com.muggle.mappers")
@SpringBootApplication(scanBasePackages = "com.muggle")
public class MuggleDriveApplication {
  public static void main(String[] args) {
    SpringApplication.run(MuggleDriveApplication.class);
  }
}
