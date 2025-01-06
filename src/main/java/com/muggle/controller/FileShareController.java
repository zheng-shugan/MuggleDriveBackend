package com.muggle.controller;

import com.muggle.entity.dto.SessionWebUserDto;
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
    PaginationResultVO listByPage = fileShareService.findListByPage(query);
    return getSuccessResponseVO(listByPage);
  }
}
