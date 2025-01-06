package com.muggle.service.impl;

import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.SessionShareDto;
import com.muggle.entity.enums.PageSize;
import com.muggle.entity.enums.ResponseCodeEnum;
import com.muggle.entity.enums.ShareValidTypeEnums;
import com.muggle.entity.po.FileShare;
import com.muggle.entity.query.FileShareQuery;
import com.muggle.entity.query.SimplePage;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.exception.BusinessException;
import com.muggle.mappers.FileShareMapper;
import com.muggle.service.FileShareService;
import com.muggle.utils.DateUtil;
import com.muggle.utils.StringTools;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 分享信息 业务接口实现 */
@Service("fileShareService")
public class FileShareServiceImpl implements FileShareService {

  @Resource private FileShareMapper<FileShare, FileShareQuery> fileShareMapper;

  /** 根据条件查询列表 */
  @Override
  public List<FileShare> findListByParam(FileShareQuery param) {
    return this.fileShareMapper.selectList(param);
  }

  /** 根据条件查询列表 */
  @Override
  public Integer findCountByParam(FileShareQuery param) {
    return this.fileShareMapper.selectCount(param);
  }

  /** 分页查询方法 */
  @Override
  public PaginationResultVO<FileShare> findListByPage(FileShareQuery param) {
    int count = this.findCountByParam(param);
    int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

    SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
    param.setSimplePage(page);
    List<FileShare> list = this.findListByParam(param);
    PaginationResultVO<FileShare> result =
        new PaginationResultVO(
            count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    return result;
  }

  /** 新增 */
  @Override
  public Integer add(FileShare bean) {
    return this.fileShareMapper.insert(bean);
  }

  /** 批量新增 */
  @Override
  public Integer addBatch(List<FileShare> listBean) {
    if (listBean == null || listBean.isEmpty()) {
      return 0;
    }
    return this.fileShareMapper.insertBatch(listBean);
  }

  /** 批量新增或者修改 */
  @Override
  public Integer addOrUpdateBatch(List<FileShare> listBean) {
    if (listBean == null || listBean.isEmpty()) {
      return 0;
    }
    return this.fileShareMapper.insertOrUpdateBatch(listBean);
  }

  /** 根据ShareId获取对象 */
  @Override
  public FileShare getFileShareByShareId(String shareId) {
    return this.fileShareMapper.selectByShareId(shareId);
  }

  /** 根据ShareId修改 */
  @Override
  public Integer updateFileShareByShareId(FileShare bean, String shareId) {
    return this.fileShareMapper.updateByShareId(bean, shareId);
  }

  /** 根据ShareId删除 */
  @Override
  public Integer deleteFileShareByShareId(String shareId) {
    return this.fileShareMapper.deleteByShareId(shareId);
  }

  @Override
  public void saveShare(FileShare fileShare) {
    ShareValidTypeEnums typeEnums = ShareValidTypeEnums.getByType(fileShare.getValidType());
    // 请求类型错误
    if (typeEnums == null) {
      throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
    // 如果文件不是永久分享
    if (typeEnums != ShareValidTypeEnums.FOREVER) {
      fileShare.setExpireTime(DateUtil.getAfterDate(typeEnums.getDays()));
    }
    Date currDate = new Date();
    fileShare.setShareTime(currDate);
    // 系统生成验证码
    if (StringUtils.isEmpty(fileShare.getCode())) {
      fileShare.setCode(StringTools.getRandomString(Constants.LENGTH_5));
    }
    fileShare.setShareId(StringTools.getRandomString(Constants.LENGTH_20));
    fileShare.setShowCount(Constants.ZERO);
    this.fileShareMapper.insert(fileShare);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void deleteFileShareBatch(String[] shareIdArray, String userId) {
    Integer count = this.fileShareMapper.deleteFileShareBatch(shareIdArray, userId);
    // 如果删除的数量不等于传入的数量
    if (count != shareIdArray.length) {
      throw new BusinessException(ResponseCodeEnum.CODE_600);
    }
  }

  @Override
  public SessionShareDto checkShareCode(String shareId, String shareCode) {
    FileShare fileShare = this.fileShareMapper.selectByShareId(shareId);
    // 如果分享信息不存在或者分享码不正确
    if (fileShare == null
        || (fileShare.getExpireTime() != null && fileShare.getExpireTime().before(new Date()))) {
      throw new BusinessException(ResponseCodeEnum.CODE_902);
    }
    if (!fileShare.getCode().equals(shareCode)) {
      throw new BusinessException("提取码错误");
    }
    // 更新浏览次数
    this.fileShareMapper.updateShareShowCount(shareId);

    SessionShareDto sessionShareDto = new SessionShareDto();
    sessionShareDto.setShareId(shareId);
    sessionShareDto.setShareUserId(fileShare.getUserId());
    sessionShareDto.setFileId(fileShare.getFileId());
    sessionShareDto.setExpireTime(sessionShareDto.getExpireTime());

    return sessionShareDto;
  }
}
