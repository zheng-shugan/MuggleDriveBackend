package com.muggle.controller;

import com.muggle.entity.enums.FileCategoryEnums;
import com.muggle.entity.enums.FileDelFlagEnums;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.service.FileService;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/file")
@RestController("fileInfoController")
public class FileController extends ABaseController {

  @Resource private FileService fileService;

  @RequestMapping("/loadDataList")
  public ResponseVO loadDataList(HttpSession session, FileInfoQuery query, String category) {
    FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
    if (categoryEnum != null) {
      query.setFileCategory(categoryEnum.getCategory());
    }
    query.setUserId(getUserInfoFromSession(session).getUserId());
    query.setOrderBy("last_update_time desc");
    query.setDelFlag(FileDelFlagEnums.USING.getFlag());
    PaginationResultVO resultVO = fileService.findListByPage(query);

    return getSuccessResponseVO(convert2PaginationVO(resultVO, FileInfo.class));
  }
}
