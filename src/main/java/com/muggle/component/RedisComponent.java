package com.muggle.component;

import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.DownloadFileDto;
import com.muggle.entity.dto.SysSettingsDto;
import com.muggle.entity.dto.UserSpaceDto;
import com.muggle.mappers.FileInfoMapper;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("redisComponent")
public class RedisComponent {
  @Resource private RedisUtils redisUtils;
  @Autowired private FileInfoMapper fileInfoMapper;

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

  /**
   * 保存临时文件大小
   *
   * @param userId
   * @param fileId
   * @param fileSize
   */
  public void saveTempFileSize(String userId, String fileId, Long fileSize) {
    Long currSize = getTempFileSize(userId, fileId);
    redisUtils.setex(
        Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId,
        currSize + fileSize,
        Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
  }

  /**
   * 获取临时文件大小
   *
   * @param userId
   * @param fileId
   * @return
   */
  public Long getTempFileSize(String userId, String fileId) {
    Long currSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
    return currSize;
  }

  /**
   * 从 redis 获取文件大小
   *
   * @param key
   * @return
   */
  public Long getFileSizeFromRedis(String key) {
    Object sizeObj = redisUtils.get(key);

    if (sizeObj == null) {
      return 0L;
    }
    if (sizeObj instanceof Integer) {
      return ((Integer) sizeObj).longValue();
    } else if (sizeObj instanceof Long) {
      return (Long) sizeObj;
    }

    return 0L;
  }

  /**
   * 保存下载链接
   *
   * @param code
   * @param downloadFileDto
   */
  public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
    redisUtils.setex(
        Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
  }

  /**
   * 获取下载链接
   *
   * @param code
   * @return
   */
  public DownloadFileDto getDownloadCode(String code) {
    return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
  }
}
