package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.component.RedisComponent;
import com.muggle.entity.dto.SysSettingsDto;
import com.muggle.entity.enums.FileDelFlagEnums;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.query.UserInfoQuery;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.entity.vo.UserInfoVO;
import com.muggle.service.FileService;
import com.muggle.service.UserService;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController extends CommonFileController {

  @Resource private RedisComponent redisComponent;

  @Resource private UserService userInfoService;

  @Resource private FileService fileInfoService;

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

  @RequestMapping("/loadUserList")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO loadUser(UserInfoQuery userInfoQuery) {
    userInfoQuery.setOrderBy("join_time desc");
    PaginationResultVO resultVO = userInfoService.findListByPage(userInfoQuery);
    return getSuccessResponseVO(convert2PaginationVO(resultVO, UserInfoVO.class));
  }

  /**
   * 修改用户状态
   *
   * @param userId
   * @param status
   * @return
   */
  @RequestMapping("/updateUserStatus")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO updateUserStatus(
      @VerifyParam(required = true) String userId, @VerifyParam(required = true) Integer status) {
    userInfoService.updateUserStatus(userId, status);
    return getSuccessResponseVO(null);
  }

  /**
   * 修改用户空间
   *
   * @param userId
   * @param changeSpace
   * @return
   */
  @RequestMapping("/updateUserSpace")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO updateUserSpace(
      @VerifyParam(required = true) String userId,
      @VerifyParam(required = true) Integer changeSpace) {
    userInfoService.changeUserSpace(userId, changeSpace);
    return getSuccessResponseVO(null);
  }

  /**
   * 查询所有文件
   *
   * @param query
   * @return
   */
  @RequestMapping("/loadFileList")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO loadDataList(FileInfoQuery query) {
    query.setOrderBy("last_update_time desc");
    query.setQueryNickName(true);
    query.setDelFlag(FileDelFlagEnums.USING.getFlag());
    PaginationResultVO resultVO = fileInfoService.findListByPage(query);
    return getSuccessResponseVO(resultVO);
  }

  @RequestMapping("/getFolderInfo")
  @GlobalInterceptor(checkLogin = false, checkAdmin = true, checkParam = true)
  public ResponseVO getFolderInfo(@VerifyParam(required = true) String path) {
    return super.getFolderInfo(null, path);
  }

  @RequestMapping("/getFile/{userId}/{fileId}")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public void getFile(
      HttpServletResponse response,
      @PathVariable("userId") @VerifyParam(required = true) String userId,
      @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    super.getFileByFileIdAndUserId(response, fileId, userId);
  }

  /**
   * 获取视频信息
   *
   * @param response
   * @param userId
   * @param fileId
   */
  @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public void getVideoInfo(
      HttpServletResponse response,
      @PathVariable("userId") @VerifyParam(required = true) String userId,
      @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    super.getFileByFileIdAndUserId(response, fileId, userId);
  }

  /**
   * 创建下载链接
   *
   * @param userId
   * @param fileId
   * @return
   */
  @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO createDownloadUrl(
      @PathVariable("userId") @VerifyParam(required = true) String userId,
      @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    return super.createDownloadUrl(fileId, userId);
  }

  /**
   * 下载文件
   *
   * @param request
   * @param response
   * @throws Exception
   */
  @RequestMapping("/download/{code}")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public void download(
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable("code") @VerifyParam(required = true) String code)
      throws Exception {
    super.downloadFile(request, response, code);
  }

  /**
   * 删除文件
   *
   * @param fileIdAndUserIds
   * @return
   */
  @RequestMapping("/delFile")
  @GlobalInterceptor(checkParam = true, checkAdmin = true)
  public ResponseVO delFile(@VerifyParam(required = true) String fileIdAndUserIds) {
    String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
    for (String fileIdAndUserId : fileIdAndUserIdArray) {
      String[] itemArray = fileIdAndUserId.split("_");
      fileInfoService.delFileBatch(itemArray[0], itemArray[1], true);
    }
    return getSuccessResponseVO(null);
  }
}
