package com.muggle.controller;

import com.muggle.entity.config.AppConfig;
import com.muggle.entity.constants.Constants;
import com.muggle.utils.StringTools;
import java.io.File;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

public class CommonFileController extends ABaseController {
  @Resource private AppConfig appConfig;

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
}
