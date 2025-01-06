package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.SessionShareDto;
import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.enums.FileDelFlagEnums;
import com.muggle.entity.enums.FileStatusEnums;
import com.muggle.entity.enums.ResponseCodeEnum;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.po.FileShare;
import com.muggle.entity.po.UserInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.vo.FileInfoVO;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.entity.vo.ShareInfoVO;
import com.muggle.exception.BusinessException;
import com.muggle.service.FileService;
import com.muggle.service.FileShareService;
import com.muggle.service.UserService;
import com.muggle.utils.CopyTools;
import java.util.Date;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.muggle.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/showShare")
@RestController("webShareController")
public class WebShareController extends CommonFileController {

  @Resource private FileShareService fileShareService;

  @Resource private FileService fileInfoService;

  @Resource private UserService userInfoService;

  /**
   * 获取分享信息
   *
   * @param session
   * @param shareId
   * @return
   */
  @RequestMapping("/getShareLoginInfo")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO getShareLoginInfo(
      HttpSession session, @VerifyParam(required = true) String shareId) {
    SessionShareDto sessionShareFromSession = getSessionShareFromSession(session, shareId);
    // 如果session中没有分享信息，则返回null
    if (sessionShareFromSession == null) {
      return getSuccessResponseVO(null);
    }
    ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
    // 是否是当前用户分享的文件
    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);
    if (userInfoFromSession != null
        && userInfoFromSession.getUserId().equals(sessionShareFromSession.getShareUserId())) {
      shareInfoVO.setCurrentUser(true);
    } else {
      shareInfoVO.setCurrentUser(false);
    }
    return getSuccessResponseVO(shareInfoVO);
  }

  /**
   * 获取分享信息
   *
   * @param shareId
   * @return
   */
  private ShareInfoVO getShareInfoCommon(String shareId) {
    // 获取分享信息
    FileShare share = fileShareService.getFileShareByShareId(shareId);
    // 如果分享信息为空或者分享信息已经过期，则返回错误信息
    if (null == share
        || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
      throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
    }
    // 获取分享文件信息
    ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
    // 获取文件信息
    FileInfo fileInfo =
        fileInfoService.getFileInfoByFileIdAndUserId(share.getFileId(), share.getUserId());
    // 如果文件为空或者文件已经删除，则返回错误信息
    if (fileInfo == null || !FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
      throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
    }
    // 设置文件信息
    shareInfoVO.setFileName(fileInfo.getFileName());
    UserInfo userInfo = userInfoService.getUserInfoByUserId(share.getUserId());
    shareInfoVO.setNickName(userInfo.getNickName());
    shareInfoVO.setUserId(userInfo.getUserId());
    // 返回分享信息
    return shareInfoVO;
  }

  /**
   * 校验分享码
   *
   * @param session
   * @param shareId
   * @param code
   * @return
   */
  @RequestMapping("/checkShareCode")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO checkShareCode(
      HttpSession session,
      @VerifyParam(required = true) String shareId,
      @VerifyParam(required = true) String code) {
    SessionShareDto shareSessionDto = fileShareService.checkShareCode(shareId, code);
    session.setAttribute(Constants.SESSION_SHARE_KEY + shareId, shareSessionDto);
    return getSuccessResponseVO(null);
  }


  /**
   * 获取分享文件列表
   * @param session
   * @param shareId
   * @param filePid
   * @return
   */
  @RequestMapping("/loadFileList")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO loadFileList(
      HttpSession session, @VerifyParam(required = true) String shareId, String filePid) {
    SessionShareDto shareDto = checkShare(session, shareId);
    FileInfoQuery query = new FileInfoQuery();
    if (!StringTools.isEmpty(filePid) && !Constants.ZERO_STR.equals(filePid)) {
      fileInfoService.checkRootFilePid(shareDto.getFileId(), shareDto.getShareUserId(), filePid);
    } else {
      query.setFileId(shareDto.getFileId());
    }
    query.setUserId(shareDto.getShareUserId());
    query.setOrderBy("last_update_time desc");
    query.setDelFlag(FileDelFlagEnums.USING.getFlag());

    PaginationResultVO resultVO = fileInfoService.findListByPage(query);
    return getSuccessResponseVO(convert2PaginationVO(resultVO, FileInfoVO.class));
  }

  private SessionShareDto checkShare(HttpSession session, String shareId) {
    SessionShareDto sessionShareFromSession = getSessionShareFromSession(session, shareId);
    // 如果session中没有分享信息，则返回错误信息
    if (sessionShareFromSession == null) {
      throw new BusinessException(ResponseCodeEnum.CODE_903);
    }
    // 分享已经过期
    if (sessionShareFromSession.getExpireTime() != null
        && new Date().after(sessionShareFromSession.getExpireTime())) {
      throw new BusinessException(ResponseCodeEnum.CODE_902);
    }
    return sessionShareFromSession;
  }

  /**
   * 获取目录信息
   *
   * @param session
   * @param shareId
   * @param path
   * @return
   */
  @RequestMapping("/getFolderInfo")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO getFolderInfo(
      HttpSession session,
      @VerifyParam(required = true) String shareId,
      @VerifyParam(required = true) String path) {
    SessionShareDto shareSessionDto = checkShare(session, shareId);
    return super.getFolderInfo(path, shareSessionDto.getShareUserId());
  }

  /**
   * 获取分享信息
   *
   * @param shareId
   * @return
   */
  @RequestMapping("/getShareInfo")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO getShareInfo(@VerifyParam(required = true) String shareId) {
    return getSuccessResponseVO(getShareInfoCommon(shareId));
  }

  @RequestMapping("/getFile/{shareId}/{fileId}")
  public void getFile(HttpServletResponse response, HttpSession session,
                      @PathVariable("shareId") @VerifyParam(required = true) String shareId,
                      @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    SessionShareDto shareSessionDto = checkShare(session, shareId);
    super.getFileByFileIdAndUserId(response, fileId, shareSessionDto.getShareUserId());
  }

  /**
   * 获取视频信息
   * @param response
   * @param session
   * @param shareId
   * @param fileId
   */
  @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
  public void getVideoInfo(HttpServletResponse response,
                           HttpSession session,
                           @PathVariable("shareId") @VerifyParam(required = true) String shareId,
                           @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    SessionShareDto shareSessionDto = checkShare(session, shareId);
    super.getFileByFileIdAndUserId(response, fileId, shareSessionDto.getShareUserId());
  }

  /**
   * 创建下载链接
   * @param session
   * @param shareId
   * @param fileId
   * @return
   */
  @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public ResponseVO createDownloadUrl(HttpSession session,
                                      @PathVariable("shareId") @VerifyParam(required = true) String shareId,
                                      @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    SessionShareDto shareSessionDto = checkShare(session, shareId);
    return super.createDownloadUrl(fileId, shareSessionDto.getShareUserId());
  }

  /**
   * 下载
   *
   * @param request
   * @param response
   * @throws Exception
   */
  @RequestMapping("/download/{code}")
  @GlobalInterceptor(checkLogin = false, checkParam = true)
  public void download(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("code") @VerifyParam(required = true) String code) throws Exception {
    super.downloadFile(request, response, code);
  }


  /**
   * 保存分享
   *
   * @param session
   * @param shareId
   * @param shareFileIds
   * @param myFolderId
   * @return
   */
  @RequestMapping("/saveShare")
  @GlobalInterceptor(checkParam = true)
  public ResponseVO saveShare(HttpSession session,
                              @VerifyParam(required = true) String shareId,
                              @VerifyParam(required = true) String shareFileIds,
                              @VerifyParam(required = true) String myFolderId) {
    SessionShareDto shareSessionDto = checkShare(session, shareId);
    SessionWebUserDto webUserDto = getUserInfoFromSession(session);
    if (shareSessionDto.getShareUserId().equals(webUserDto.getUserId())) {
      throw new BusinessException("自己分享的文件无法保存到自己的网盘");
    }
    fileInfoService.saveShare(shareSessionDto.getFileId(), shareFileIds, myFolderId, shareSessionDto.getShareUserId(), webUserDto.getUserId());
    return getSuccessResponseVO(null);
  }
}
