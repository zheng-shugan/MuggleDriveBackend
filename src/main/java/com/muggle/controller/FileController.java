package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.dto.UploadResultDto;
import com.muggle.entity.enums.FileCategoryEnums;
import com.muggle.entity.enums.FileDelFlagEnums;
import com.muggle.entity.enums.FileFolderTypeEnums;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.vo.FileInfoVO;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.service.FileService;
import com.muggle.utils.CopyTools;
import com.muggle.utils.StringTools;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/file")
@RestController("fileInfoController")
public class FileController extends CommonFileController {

  private static final Logger logger = LoggerFactory.getLogger(FileController.class);
  @Resource private FileService fileService;

  /**
   * 获取文件列表
   *
   * @param session
   * @param query
   * @param category
   * @return
   */
  @RequestMapping("/loadDataList")
  public ResponseVO loadDataList(HttpSession session, FileInfoQuery query, String category) {
    FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
    if (null != categoryEnum) {
      query.setFileCategory(categoryEnum.getCategory());
    }
    query.setUserId(getUserInfoFromSession(session).getUserId());
    query.setOrderBy("last_update_time desc");
    query.setDelFlag(FileDelFlagEnums.USING.getFlag());
    PaginationResultVO resultVO = fileService.findListByPage(query);
    return getSuccessResponseVO(convert2PaginationVO(resultVO, FileInfoVO.class));
  }

  /**
   * 上传文件
   *
   * @param session
   * @param fileId 文件ID
   * @param filePid 文件父ID
   * @param fileMd5 文件MD5
   * @param chunkIndex 当前块索引
   * @param chunks 总块数
   * @return
   */
  @RequestMapping("/uploadFile")
  // @GlobalInterceptor(checkParam = true)
  public ResponseVO uploadFile(
      HttpSession session,
      MultipartFile file,
      String fileId,
      @VerifyParam(required = false) String fileName,
      @VerifyParam(required = true) String filePid,
      @VerifyParam(required = true) String fileMd5,
      @VerifyParam(required = true) Integer chunkIndex,
      @VerifyParam(required = true) Integer chunks) {

    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);
    UploadResultDto resultDto =
        fileService.uploadFile(
            userInfoFromSession, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);

    return getSuccessResponseVO(resultDto);
  }

  /**
   * 获取图片
   *
   * @param response
   * @param imageFolder
   * @param imageName
   */
  @RequestMapping("getImage/{imageFolder}/{imageName}")
  // @GlobalInterceptor(checkParam = true)
  public void getImage(
      HttpServletResponse response,
      @PathVariable("imageFolder") String imageFolder,
      @PathVariable("imageName") String imageName) {
    getImageByFolderAndName(response, imageFolder, imageName);
  }

  /**
   * 获取视频信息
   *
   * @param response
   * @param session
   * @param fileId
   */
  @RequestMapping("/ts/getVideoInfo/{fileId}")
  public void getVideoInfo(
      HttpServletResponse response,
      HttpSession session,
      @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);
    getFileByFileIdAndUserId(response, fileId, userInfoFromSession.getUserId());
  }

  /**
   * 获取普通文件
   *
   * @param response
   * @param session
   * @param fileId
   */
  @RequestMapping("/getFile/{fileId}")
  public void getCommonFile(
      HttpServletResponse response,
      HttpSession session,
      @PathVariable("fileId") @VerifyParam(required = true) String fileId) {
    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);
    getFileByFileIdAndUserId(response, fileId, userInfoFromSession.getUserId());
  }

  @RequestMapping("/newFolder")
  public ResponseVO newFolder(
      HttpSession session,
      @VerifyParam(required = true) String fileId,
      @VerifyParam(required = true) String filePid,
      @VerifyParam(required = true) String fileName) {

    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);

    FileInfo fileInfo = fileService.newFolder(userInfoFromSession.getUserId(), filePid, fileName);

    return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
  }

  @RequestMapping(("/getFolderInfo"))
  public ResponseVO getFolderInfo(HttpSession session, @VerifyParam(required = true) String path) {

    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);

    return super.getFolderInfo(userInfoFromSession.getUserId(), path);
  }

  @RequestMapping(("/rename"))
  public ResponseVO rename(
      HttpSession session,
      @VerifyParam(required = true) String fileId,
      @VerifyParam(required = true) String filePid,
      @VerifyParam(required = true) String fileName) {

    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);

    FileInfo fileInfo =
        fileService.rename(userInfoFromSession.getUserId(), filePid, fileId, fileName);

    return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
  }

  /**
   * 获取所有目录
   *
   * @param session
   * @param filePid
   * @param currentFileIds
   * @return
   */
  @RequestMapping(("/loadAllFolder"))
  public ResponseVO loadAllFolder(
      HttpSession session, @VerifyParam(required = true) String filePid, String currentFileIds) {

    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);

    FileInfoQuery query = new FileInfoQuery();
    query.setUserId(userInfoFromSession.getUserId());
    query.setFilePid(filePid);
    query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
    if (!StringTools.isEmpty(currentFileIds)) {
      query.setExcludeFileIdArray(currentFileIds.split(","));
    }
    query.setDelFlag(FileDelFlagEnums.USING.getFlag());
    query.setOrderBy("create_time desc");
    List<FileInfo> fileInfoList = fileService.findListByParam(query);

    return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FileInfoVO.class));
  }

  /**
   * 改变目录
   *
   * @param session
   * @param filePid
   * @param fileIds
   * @return
   */
  @RequestMapping(("/changeFileFolder"))
  public ResponseVO changeFileFolder(
      HttpSession session, String fileIds, @VerifyParam(required = true) String filePid) {

    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);

    fileService.changeFileFolder(fileIds, filePid, userInfoFromSession.getUserId());

    return getSuccessResponseVO(null);
  }

  /**
   * 获取下载链接
   *
   * @param session
   * @param fileId
   * @return
   */
  @RequestMapping(("/createDownloadUrl/{fileId}"))
  public ResponseVO createDownloadUrl(
      HttpSession session, @VerifyParam(required = true) @PathVariable("fileId") String fileId) {

    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);

    ResponseVO downloadUrl = super.createDownloadUrl(fileId, userInfoFromSession.getUserId());

    return getSuccessResponseVO(downloadUrl);
  }

  @RequestMapping("/download/{code}")
  @GlobalInterceptor(checkParam = true, checkLogin = false)
  public void downloadFile(
      HttpServletRequest request, HttpServletResponse response, @PathVariable("code") String code)
      throws Exception {
    super.downloadFile(request, response, code);
  }

  @RequestMapping("/delFile")
  @GlobalInterceptor(checkParam = true, checkLogin = true)
  public ResponseVO delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
    SessionWebUserDto webUserDto = getUserInfoFromSession(session);
    fileService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
    return getSuccessResponseVO(null);
  }
}
