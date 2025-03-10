package com.muggle.service;

import com.muggle.entity.dto.SessionShareDto;
import com.muggle.entity.po.FileShare;
import com.muggle.entity.query.FileShareQuery;
import com.muggle.entity.vo.PaginationResultVO;
import java.util.List;

/** 分享信息 业务接口 */
public interface FileShareService {

  /** 根据条件查询列表 */
  List<FileShare> findListByParam(FileShareQuery param);

  /** 根据条件查询列表 */
  Integer findCountByParam(FileShareQuery param);

  /** 分页查询 */
  PaginationResultVO<FileShare> findListByPage(FileShareQuery param);

  /** 新增 */
  Integer add(FileShare bean);

  /** 批量新增 */
  Integer addBatch(List<FileShare> listBean);

  /** 批量新增/修改 */
  Integer addOrUpdateBatch(List<FileShare> listBean);

  /** 根据ShareId查询对象 */
  FileShare getFileShareByShareId(String shareId);

  /** 根据ShareId修改 */
  Integer updateFileShareByShareId(FileShare bean, String shareId);

  /** 根据ShareId删除 */
  Integer deleteFileShareByShareId(String shareId);

  /**
   * 分享文件
   *
   * @param fileShare
   */
  void saveShare(FileShare fileShare);

  /**
   * 取消分享
   *
   * @param shareIdArray
   * @param userId
   */
  void deleteFileShareBatch(String[] shareIdArray, String userId);

  /**
   * 检查分享码
   *
   * @param shareId
   * @param shareCode
   * @return
   */
  SessionShareDto checkShareCode(String shareId, String shareCode);
}
