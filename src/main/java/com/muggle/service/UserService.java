package com.muggle.service;

import com.muggle.entity.po.UserInfo;
import com.muggle.entity.query.UserInfoQuery;

import java.util.List;

public interface UserService {
    public List<UserInfo> findListByParam(UserInfoQuery userInfoQuery);
}
