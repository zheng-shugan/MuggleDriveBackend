package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.enums.FileDelFlagEnums;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.vo.FileInfoVO;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.service.FileService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * 回收站
 */
@RestController("RecycleController")
@RequestMapping("/recycle")
public class RecycleController extends ABaseController {
  @Resource private FileService fileService;

  @RequestMapping("/loadRecycleList")
  @GlobalInterceptor
  public ResponseVO loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize) {
    FileInfoQuery query = new FileInfoQuery();
    query.setPageNo(pageNo);
    query.setPageSize(pageSize);
    query.setUserId(getUserInfoFromSession(session).getUserId());
    query.setOrderBy("recovery_time desc");
    query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
    PaginationResultVO<FileInfo> listByPage = fileService.findListByPage(query);
    return getSuccessResponseVO(convert2PaginationVO(listByPage, FileInfoVO.class));
  }

  /**
   * 从回收站恢复文件
   * @param session
   * @param fileIds
   * @return
   */
  @RequestMapping("/recoverFile")
  @GlobalInterceptor(checkParam = true)
  public ResponseVO recoverFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
    SessionWebUserDto webUserDto = getUserInfoFromSession(session);
    fileService.recoverFileBatch(webUserDto.getUserId(), fileIds);
    return getSuccessResponseVO(null);
  }
}
