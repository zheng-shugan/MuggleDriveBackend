package com.muggle.component;

import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.SysSettingsDto;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component("redisComponent")
public class RedisComponent {
  @Resource private RedisUtils redisUtils;

  /**
   * 获取系统设置
   *
   * @return
   */
  public SysSettingsDto getSysSettings() {
    SysSettingsDto sysSettingsDto =
        (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
    if (sysSettingsDto == null) {
      sysSettingsDto = new SysSettingsDto();
      redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
    }

    return sysSettingsDto;
  }
}
