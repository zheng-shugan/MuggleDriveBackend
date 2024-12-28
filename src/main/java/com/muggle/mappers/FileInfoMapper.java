package com.muggle.mappers;

import com.muggle.entity.po.FileInfo;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 文件信息 数据库操作接口 */
public interface FileInfoMapper<T, P> extends BaseMapper<T, P> {

  /** 根据FileIdAndUserId更新 */
  Integer updateByFileIdAndUserId(
      @Param("bean") T t, @Param("fileId") String fileId, @Param("userId") String userId);

  /** 根据FileIdAndUserId删除 */
  Integer deleteByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

  /** 根据FileIdAndUserId获取对象 */
  T selectByFileIdAndUserId(@Param("fileId") String fileId, @Param("userId") String userId);

  void updateFileStatusWithOldStatus(
      @Param("fileId") String fileId,
      @Param("userId") String userId,
      @Param("bean") T t,
      @Param("oldStatus") Integer oldStatus);

  void updateFileDelFlagBatch(
      @Param("bean") FileInfo fileInfo,
      @Param("userId") String userId,
      @Param("filePidList") List<String> filePidList,
      @Param("fileIdList") List<String> fileIdList,
      @Param("oldDelFlag") Integer oldDelFlag);

  void delFileBatch(
      @Param("userId") String userId,
      @Param("filePidList") List<String> filePidList,
      @Param("fileIdList") List<String> fileIdList,
      @Param("oldDelFlag") Integer oldDelFlag);

  /**
   * 查询用户使用空间
   *
   * @param userId
   * @return
   */
  Long selectUseSpace(@Param("userId") String userId);

  /**
   * 删除用户文件
   *
   * @param userId
   */
  void deleteFileByUserId(@Param("userId") String userId);
}
