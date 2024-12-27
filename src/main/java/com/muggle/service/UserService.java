package com.muggle.service;

import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.po.UserInfo;
import com.muggle.entity.query.UserInfoQuery;
import com.muggle.entity.vo.PaginationResultVO;
import java.util.List;

public interface UserService {

  /** 根据条件查询列表 */
  Integer findCountByParam(UserInfoQuery param);

  /** 分页查询 */
  PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param);

  /** 新增 */
  Integer add(UserInfo bean);

  /** 批量新增 */
  Integer addBatch(List<UserInfo> listBean);

  /** 批量新增/修改 */
  Integer addOrUpdateBatch(List<UserInfo> listBean);

  /** 根据UserId查询对象 */
  UserInfo getUserInfoByUserId(String userId);

  /** 根据UserId修改 */
  Integer updateUserInfoByUserId(UserInfo bean, String userId);

  /** 根据UserId删除 */
  Integer deleteUserInfoByUserId(String userId);

  /** 根据Email查询对象 */
  UserInfo getUserInfoByEmail(String email);

  /** 根据Email修改 */
  Integer updateUserInfoByEmail(UserInfo bean, String email);

  /** 根据Email删除 */
  Integer deleteUserInfoByEmail(String email);

  /** 根据NickName查询对象 */
  UserInfo getUserInfoByNickName(String nickName);

  /** 根据NickName修改 */
  Integer updateUserInfoByNickName(UserInfo bean, String nickName);

  /** 根据NickName删除 */
  Integer deleteUserInfoByNickName(String nickName);

  /** 根据条件查询列表 */
  public List<UserInfo> findListByParam(UserInfoQuery userInfoQuery);

  /** 注册 */
  public void register(String email, String nickname, String password, String emailCode);

  /** 登录 */
  public SessionWebUserDto login(String email, String password);
}
