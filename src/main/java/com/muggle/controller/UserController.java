package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.component.RedisComponent;
import com.muggle.entity.config.AppConfig;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.CreateImageCode;
import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.dto.UserSpaceDto;
import com.muggle.entity.enums.VerifyRegexEnum;
import com.muggle.entity.po.UserInfo;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.exception.BusinessException;
import com.muggle.service.EmailCodeService;
import com.muggle.service.UserService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController("/user")
public class UserController extends ABaseController {

  private static final Logger logger = LoggerFactory.getLogger(UserController.class);

  @Resource private UserService userInfoService;

  @Resource private EmailCodeService emailCodeService;

  @Resource private AppConfig appConfig;

  @Resource private RedisComponent redisComponent;

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
      @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
      @VerifyParam(required = true) String checkCode,
      @VerifyParam(required = true) Integer type) {
    try {
      // 如果checkCode与session的checkCode不一样
      if (!checkCode.equals(session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
        throw new BusinessException("验证码错误");
      }

      // 发送验证码
      emailCodeService.sendEmailCode(email, type);
      return getSuccessResponseVO(null);
    } finally {
      // 清除验证码
      session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
    }
  }

  @RequestMapping("/register")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO register(
      HttpSession session,
      @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
      @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18)
          String password,
      @VerifyParam(required = true) String nickName,
      @VerifyParam(required = true) String checkCode,
      @VerifyParam(required = true) String emailCode) {
    try {
      // 如果checkCode与session的checkCode不一样
      if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
        throw new BusinessException("图片验证码不正确");
      }
      userInfoService.register(email, password, nickName, emailCode);
      return getSuccessResponseVO(null);
    } finally {
      // 清除验证码
      session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
    }
  }

  @RequestMapping("/login")
  @GlobalInterceptor(checkParam = true)
  public ResponseVO login(
      HttpSession session,
      @VerifyParam(required = true) String email,
      @VerifyParam(required = true) String password,
      @VerifyParam(required = true) String checkCode) {
    try {
      // 如果checkCode与session的checkCode不一样
      if (!checkCode.equals(session.getAttribute(Constants.CHECK_CODE_KEY))) {
        throw new BusinessException("验证码错误");
      }

      SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password);
      session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);

      return getSuccessResponseVO(sessionWebUserDto);
    } finally {
      // 清除验证码
      session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
    }
  }

  @RequestMapping("/resetPassword")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO resetPassword(
      HttpSession session,
      @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
      @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18)
          String password,
      @VerifyParam(required = true) String checkCode,
      @VerifyParam(required = true) String emailCode) {
    try {
      // 如果checkCode与session的checkCode不一样
      if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
        throw new BusinessException("图片验证码不正确");
      }

      // 修改密码
      userInfoService.resetPassword(email, password, emailCode);
      return getSuccessResponseVO(null);
    } finally {
      // 清除验证码
      session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
    }
  }

  @RequestMapping("/getAvatar/{userId}")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public void getAvatar(
      HttpServletResponse response,
      @VerifyParam(required = true) @PathVariable("userId") String userId) {

    String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
    File folder = new File(appConfig.getProjectFolder() + avatarFolderName);

    if (!folder.exists()) {
      folder.mkdirs();
    }

    String avatarPath =
        appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
    File file = new File(avatarPath);

    if (!file.exists()) {
      if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT)
          .exists()) {
        logger.error(
            "头像路径不存在:{}",
            appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT);
        printNoDefaultImage(response);
        return;
      }

      avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
    }
    response.setContentType("image/jpg");
    readFile(response, avatarPath);
  }

  @RequestMapping("/updateUserAvatar")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {

    SessionWebUserDto webUserDto = getUserInfoFromSession(session);

    String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
    File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
    if (!targetFileFolder.exists()) {
      targetFileFolder.mkdirs();
    }
    String userId = webUserDto.getUserId();
    File targetFile = new File(targetFileFolder.getPath() + "/" + userId + Constants.AVATAR_SUFFIX);
    try {
      avatar.transferTo(targetFile);
    } catch (Exception e) {
      logger.error("上传头像失败", e);
    }

    UserInfo userInfo = new UserInfo();

    // userInfoService.updateUserInfoByUserId(userInfo, userId);
    webUserDto.setAvatar(null);
    session.setAttribute(Constants.SESSION_KEY, webUserDto);
    return getSuccessResponseVO(null);
  }

  private void printNoDefaultImage(HttpServletResponse response) {
    response.setHeader(Constants.CONTENT_TYPE, Constants.CONTENT_TYPE_VALUE);
    response.setStatus(HttpStatus.OK.value());
    PrintWriter writer = null;
    try {
      writer = response.getWriter();
      writer.print("请在头像目录下放置默认头像default_avatar.jpg");
      writer.close();
    } catch (Exception e) {
      logger.error("输出无默认图失败", e);
    } finally {
      writer.close();
    }
  }

  @RequestMapping("/getUserInfo")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO getUserInfo(HttpSession session) {
    SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
    return getSuccessResponseVO(sessionWebUserDto);
  }

  @RequestMapping("/getUseSpace")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO getUserSpace(HttpSession session) {
    SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);

    UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(sessionWebUserDto.getUserId());

    return getSuccessResponseVO(userSpaceDto);
  }

  @RequestMapping("/logout")
  @GlobalInterceptor(checkLogin = true, checkParam = true)
  public ResponseVO logout(HttpSession session) {
    // 清除 session
    session.invalidate();
    return getSuccessResponseVO(null);
  }
}
