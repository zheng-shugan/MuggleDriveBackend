package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.component.RedisComponent;
import com.muggle.entity.dto.SysSettingsDto;
import com.muggle.entity.vo.ResponseVO;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController extends ABaseController {

  @Resource private RedisComponent redisComponent;

  /**
   * 获取系统设置
   *
   * @return
   */
  @RequestMapping("/getSysSettings")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO getSysSettings() {
    return getSuccessResponseVO(redisComponent.getSysSettings());
  }

  /**
   * 保存系统设置
   *
   * @param registerEmailTitle
   * @param registerEmailContent
   * @param userInitUseSpace
   * @return
   */
  @RequestMapping("/saveSysSettings")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO saveSysSettings(
      @VerifyParam(required = true) String registerEmailTitle,
      @VerifyParam(required = true) String registerEmailContent,
      @VerifyParam(required = true) Integer userInitUseSpace) {

    SysSettingsDto sysSettingsDto = new SysSettingsDto();

    sysSettingsDto.setRegisterEmailTitle(registerEmailTitle);
    sysSettingsDto.setRegisterEmailContent(registerEmailContent);
    sysSettingsDto.setUserInitUseSpace(userInitUseSpace);
    redisComponent.saveSysSettings(sysSettingsDto);

    return getSuccessResponseVO(null);
  }


}
