package com.muggle.controller;

import com.muggle.annotation.GlobalInterceptor;
import com.muggle.annotation.VerifyParam;
import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.po.FileShare;
import com.muggle.entity.query.FileShareQuery;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.entity.vo.ResponseVO;
import com.muggle.service.FileShareService;
import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/share")
@RestController("shareController")
public class FileShareController extends ABaseController {

  @Resource private FileShareService fileShareService;

  @RequestMapping("/loadShareList")
  public ResponseVO loadShareList(HttpSession session, FileShareQuery query) {
    query.setOrderBy("share_time desc");
    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);
    query.setUserId(userInfoFromSession.getUserId());
    query.setQueryFileName(true);
    PaginationResultVO listByPage = fileShareService.findListByPage(query);
    return getSuccessResponseVO(listByPage);
  }

  @RequestMapping("/shareFile")
  @GlobalInterceptor(checkParam = true)
  public ResponseVO shareFile(
      HttpSession session,
      @VerifyParam(required = true) String fileId,
      @VerifyParam(required = true) Integer validType,
      String code) {
    SessionWebUserDto userInfoFromSession = getUserInfoFromSession(session);

    FileShare fileShare = new FileShare();
    fileShare.setFileId(fileId);
    fileShare.setUserId(userInfoFromSession.getUserId());
    fileShare.setCode(code);
    fileShare.setValidType(validType);

    fileShareService.saveShare(fileShare);

    return getSuccessResponseVO(fileShare);
  }

  @RequestMapping("/cancelShare")
  @GlobalInterceptor(checkParam = true)
  public ResponseVO cancelShare(HttpSession session, @VerifyParam(required = true) String shareIds) {
    SessionWebUserDto userDto = getUserInfoFromSession(session);
    fileShareService.deleteFileShareBatch(shareIds.split(","), userDto.getUserId());
    return getSuccessResponseVO(null);
  }
}
