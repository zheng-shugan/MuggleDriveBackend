package com.muggle.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

public class TestController {
  @RequestMapping("test")
  public String test() {
    return "Hello!";
  }
}
