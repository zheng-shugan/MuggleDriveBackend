package com.muggle.service.impl;

import com.muggle.entity.po.UserInfo;
import com.muggle.entity.query.UserInfoQuery;
import com.muggle.mappers.UserInfoMapper;
import com.muggle.service.UserService;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserServiceImpl implements UserService {

  @Resource private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

  /**
   * 根据条件查询列表
   *
   * @param userInfoQuery
   * @return
   */
  @Override
  public List<UserInfo> findListByParam(UserInfoQuery userInfoQuery) {
    return userInfoMapper.selectList(userInfoQuery);
  }
}
