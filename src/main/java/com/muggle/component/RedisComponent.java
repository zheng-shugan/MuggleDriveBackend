package com.muggle.component;

import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.SysSettingsDto;
import com.muggle.entity.dto.UserSpaceDto;
import javax.annotation.Resource;

import com.muggle.mappers.FileInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("redisComponent")
public class RedisComponent {
  @Resource private RedisUtils redisUtils;
  @Autowired
  private FileInfoMapper fileInfoMapper;

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

  /**
   * 保存用户空间使用情况
   *
   * @param userID
   * @param userSpaceDto
   */
  public void saveUserSpaceUse(String userID, UserSpaceDto userSpaceDto) {
    redisUtils.setex(
        Constants.REDIS_KEY_USER_SPACE_USE + userID, userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
  }

  /**
   * 获取用户空间使用情况
   *
   * @param userID
   * @return
   */
  public UserSpaceDto getUserSpaceDto(String userID) {
    UserSpaceDto userSpaceDto =
        (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userID);

    if (userSpaceDto == null) {
      userSpaceDto = new UserSpaceDto();
      Long useSpace = fileInfoMapper.selectUseSpace(userID);
      userSpaceDto.setUseSpace(useSpace);
      userSpaceDto.setTotalSpace(getSysSettings().getUserInitUseSpace() * Constants.MB);

      this.saveUserSpaceUse(userID, userSpaceDto);
    }

    return userSpaceDto;
  }
}
