package com.muggle.service;

public interface EmailCodeService {

  void sendEmailCode(String email, Integer type);

  void checkEmailCode(String email, String code);
}
