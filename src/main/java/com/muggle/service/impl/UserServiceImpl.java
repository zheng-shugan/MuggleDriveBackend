package com.muggle.service.impl;

import com.muggle.component.RedisComponent;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.SysSettingsDto;
import com.muggle.entity.enums.PageSize;
import com.muggle.entity.po.UserInfo;
import com.muggle.entity.query.SimplePage;
import com.muggle.entity.query.UserInfoQuery;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.exception.BusinessException;
import com.muggle.mappers.UserInfoMapper;
import com.muggle.service.EmailCodeService;
import com.muggle.service.UserService;
import com.muggle.utils.StringTools;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("userService")
public class UserServiceImpl implements UserService {

  @Resource private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

  @Resource private EmailCodeService emailCodeService;

  @Resource private RedisComponent redisComponent;

  /** 根据条件查询列表 */
  @Override
  public Integer findCountByParam(UserInfoQuery param) {
    return this.userInfoMapper.selectCount(param);
  }

  /** 分页查询方法 */
  @Override
  public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
    int count = this.findCountByParam(param);
    int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

    SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
    param.setSimplePage(page);
    List<UserInfo> list = this.findListByParam(param);
    PaginationResultVO<UserInfo> result =
        new PaginationResultVO(
            count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    return result;
  }

  /** 新增 */
  @Override
  public Integer add(UserInfo bean) {
    return this.userInfoMapper.insert(bean);
  }

  /** 批量新增 */
  @Override
  public Integer addBatch(List<UserInfo> listBean) {
    if (listBean == null || listBean.isEmpty()) {
      return 0;
    }
    return this.userInfoMapper.insertBatch(listBean);
  }

  /** 批量新增或者修改 */
  @Override
  public Integer addOrUpdateBatch(List<UserInfo> listBean) {
    if (listBean == null || listBean.isEmpty()) {
      return 0;
    }
    return this.userInfoMapper.insertOrUpdateBatch(listBean);
  }

  /** 根据UserId获取对象 */
  @Override
  public UserInfo getUserInfoByUserId(String userId) {
    return this.userInfoMapper.selectByUserId(userId);
  }

  /** 根据UserId修改 */
  @Override
  public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
    return this.userInfoMapper.updateByUserId(bean, userId);
  }

  /** 根据UserId删除 */
  @Override
  public Integer deleteUserInfoByUserId(String userId) {
    return this.userInfoMapper.deleteByUserId(userId);
  }

  /** 根据Email获取对象 */
  @Override
  public UserInfo getUserInfoByEmail(String email) {
    return this.userInfoMapper.selectByEmail(email);
  }

  /** 根据Email修改 */
  @Override
  public Integer updateUserInfoByEmail(UserInfo bean, String email) {
    return this.userInfoMapper.updateByEmail(bean, email);
  }

  /** 根据Email删除 */
  @Override
  public Integer deleteUserInfoByEmail(String email) {
    return this.userInfoMapper.deleteByEmail(email);
  }

  /** 根据NickName获取对象 */
  @Override
  public UserInfo getUserInfoByNickName(String nickName) {
    return this.userInfoMapper.selectByNickName(nickName);
  }

  /** 根据NickName修改 */
  @Override
  public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
    return this.userInfoMapper.updateByNickName(bean, nickName);
  }

  /** 根据NickName删除 */
  @Override
  public Integer deleteUserInfoByNickName(String nickName) {
    return this.userInfoMapper.deleteByNickName(nickName);
  }

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

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void register(String email, String nickname, String password, String emailCode) {
    UserInfo userInfo = this.getUserInfoByEmail(email);
    if (userInfo != null) {
      throw new BusinessException("邮箱已存在");
    }

    UserInfo nicknameUser = this.getUserInfoByNickName(nickname);
    if (nicknameUser != null) {
      throw new BusinessException("昵称已存在");
    }

    // 校验邮箱验证码
    emailCodeService.checkEmailCode(email, emailCode);

    // 设置用户信息
    UserInfo user = new UserInfo();
    String userID = StringTools.getRandomString(Constants.LENGTH_10);
    userInfo.setUserId(userID);
    user.setEmail(email);
    user.setNickName(nickname);
    user.setPassword(StringTools.encodeByMD5(password));
    user.setJoinTime(new Date());
    user.setUseSpace(0L);
    SysSettingsDto sysSettingsDto = redisComponent.getSysSettings();
    user.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
    // 添加到数据库
    this.userInfoMapper.insert(user);
  }
}
