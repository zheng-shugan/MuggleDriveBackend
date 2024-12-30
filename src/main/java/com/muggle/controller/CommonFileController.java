package com.muggle.controller;

import com.muggle.entity.config.AppConfig;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.enums.FileCategoryEnums;
import com.muggle.entity.po.FileInfo;
import com.muggle.service.FileService;
import com.muggle.utils.StringTools;
import java.io.File;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

public class CommonFileController extends ABaseController {
  @Resource private AppConfig appConfig;

  @Resource private FileService fileService;

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
}
