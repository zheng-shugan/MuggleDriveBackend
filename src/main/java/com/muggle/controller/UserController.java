package com.muggle.controller;

import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.CreateImageCode;
import com.muggle.service.UserService;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/user")
public class UserController {
  @Resource private UserService userInfoService;

  @RequestMapping("/checkCode")
  public void checkCode(HttpServletResponse response, HttpSession session, Integer type)
      throws IOException {
    CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Cache-Control", "no-cache");
    response.setDateHeader("Expires", 0);
    response.setContentType("image/jpeg");
    String code = vCode.getCode();
    if (type == null || type == 0) {
      session.setAttribute(Constants.CHECK_CODE_KEY, code);
    } else {
      session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
    }
    vCode.write(response.getOutputStream());
  }
}
