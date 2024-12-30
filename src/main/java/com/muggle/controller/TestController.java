package com.muggle.controller;

import com.muggle.exception.BusinessException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  @RequestMapping("test")
  public String test() {
    return "Hello!";
  }

  @RequestMapping("/test/error")
  public void testError() {
    throw new BusinessException("Test Error!");
  }
}
