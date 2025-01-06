package com.muggle.controller;

import com.muggle.component.RedisComponent;
import com.muggle.entity.config.AppConfig;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.DownloadFileDto;
import com.muggle.entity.enums.FileCategoryEnums;
import com.muggle.entity.enums.FileFolderTypeEnums;
import com.muggle.entity.enums.ResponseCodeEnum;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.vo.FileInfoVO;
import com.muggle.entity.vo.FolderVO;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.exception.BusinessException;
import com.muggle.service.FileService;
import com.muggle.utils.CopyTools;
import com.muggle.utils.StringTools;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

public class CommonFileController extends ABaseController {
  @Resource private AppConfig appConfig;

  @Resource private FileService fileService;

  @Resource private RedisComponent redisComponent;

  /**
   * 根据路径和图片名字获取图片
   *
   * @param response
   * @param imageFolder
   * @param imageName
   */
  public void getImageByFolderAndName(
      HttpServletResponse response, String imageFolder, String imageName) {
    // 路径为空
    if (StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName)) {
      return;
    }
    // 路径不对
    if (!StringTools.pathIsOk(imageFolder) || !StringTools.pathIsOk(imageName)) {
      return;
    }
    String imageSuffix = StringTools.getFileSuffix(imageName);
    String filePath =
        appConfig.getProjectFolder()
            + Constants.FILE_FOLDER_FILE
            + imageFolder
            + File.separator
            + imageName;
    imageSuffix = imageSuffix.replace(".", "");
    String contentType = "image/" + imageSuffix;
    response.setContentType(contentType);
    response.setHeader("Cache-Control", "max-age=31536000");
    readFile(response, filePath);
  }

  /**
   * 根据文件名和用户获取文件
   *
   * @param response
   * @param fileId
   * @param userId
   */
  protected void getFileByFileIdAndUserId(
      HttpServletResponse response, String fileId, String userId) {
    String filePath = null;
    // 如果请求的是视频分片文件
    if (fileId.endsWith(".ts")) {
      String[] tsArray = fileId.split("_");
      String realFileId = tsArray[0];
      FileInfo fileInfo = fileService.getFileInfoByFileIdAndUserId(realFileId, userId);
      // 文件不存在
      if (fileInfo == null) {
        return;
      }
      String fileName = fileInfo.getFilePath();
      fileName = StringTools.getFileNameNoSuffix(fileName) + File.separator + fileId;
      filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
    } else {
      FileInfo fileInfo = fileService.getFileInfoByFileIdAndUserId(fileId, userId);
      // 文件不存在
      if (fileInfo == null) {
        return;
      }
      // 读取 .m3u8 文件
      if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
        String fileNameWithNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
        filePath =
            appConfig.getProjectFolder()
                + Constants.FILE_FOLDER_FILE
                + fileNameWithNoSuffix
                + File.separator
                + Constants.M3U8_NAME;
      } else { // 读取普通文件
        filePath =
            appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
      }
      File file = new File(filePath);
      if (!file.exists()) {
        return;
      }
    }
    readFile(response, filePath);
  }

  /**
   * 获取目录信息
   *
   * @param userId
   * @param path
   * @return
   */
  public ResponseVO getFolderInfo(String userId, String path) {
    String[] pathList = path.split("/");
    FileInfoQuery query = new FileInfoQuery();
    query.setUserId(userId);
    query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
    query.setFileIdArray(pathList);
    String orderBy = "field(file_id,\"" + StringUtils.join(pathList, "\",\"") + "\")";
    query.setOrderBy(orderBy);
    List<FileInfo> fileInfoList = fileService.findListByParam(query);
    return getSuccessResponseVO(CopyTools.copyList(fileInfoList, FolderVO.class));
  }

  /**
   * 获取下载链接
   *
   * @param userId
   * @param fileId
   * @return
   */
  protected ResponseVO createDownloadUrl(String fileId, String userId) {
    FileInfo fileInfo = fileService.getFileInfoByFileIdAndUserId(fileId, userId);

    // 如果文件不存在或者是下载的是文件夹
    if (fileInfo == null || FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
      throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    // 创建下载链接
    String code = StringTools.getRandomString(Constants.LENGTH_50);

    DownloadFileDto downloadFileDto = new DownloadFileDto();
    downloadFileDto.setDownloadCode(code);
    downloadFileDto.setFileId(fileId);
    downloadFileDto.setFilePath(fileInfo.getFilePath());
    downloadFileDto.setFileName(fileInfo.getFileName());
    // 保存到 redis
    redisComponent.saveDownloadCode(code, downloadFileDto);
    return getSuccessResponseVO(code);
  }

  /**
   * 下载文件
   * @param request
   * @param response
   * @param code
   * @throws Exception
   */
    protected void downloadFile(HttpServletRequest request, HttpServletResponse response, String code)
      throws Exception {
    DownloadFileDto downloadFileDto = redisComponent.getDownloadCode(code);
    if (null == downloadFileDto) {
      return;
    }
    String filePath =
        appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
    String fileName = downloadFileDto.getFileName();
    response.setContentType("application/x-msdownload; charset=UTF-8");
    if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) { // IE浏览器
      fileName = URLEncoder.encode(fileName, "UTF-8");
    } else {
      fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
    }
    response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
    readFile(response, filePath);
  }
}
