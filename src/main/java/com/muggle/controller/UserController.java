package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.CreateImageCode;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.exception.BusinessException;
import com.muggle.service.EmailCodeService;
import com.muggle.service.UserService;
import java.io.IOException;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/user")
public class UserController extends ABaseController {
  @Resource private UserService userInfoService;

  @Resource private EmailCodeService emailCodeService;

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

  @RequestMapping("/sendEmailCode")
  @GlobalInterceptor(checkParam = true)
  public ResponseVO sendEmailCode(
      HttpSession session,
      @VerifyParam(required = true) String email,
      @VerifyParam(required = true) String checkCode,
      @VerifyParam(required = true) Integer type) {
    try {
      // 如果checkCode与session的checkCode不一样
      if (!checkCode.equals(session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
        throw new BusinessException("验证码错误");
      }

      // 发送验证码
      // emailCodeService.sendEmailCode(email, type);
      return getSuccessResponseVO(null);
    } finally {
      // 清除验证码
      session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
    }
  }
}
