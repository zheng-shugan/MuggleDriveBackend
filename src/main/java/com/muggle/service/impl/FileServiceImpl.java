package com.muggle.service.impl;

import com.muggle.entity.enums.PageSize;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.query.SimplePage;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.mappers.FileInfoMapper;
import com.muggle.service.FileService;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component("fileService")
public class FileServiceImpl implements FileService {

  @Resource private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

  /** 根据条件查询列表 */
  @Override
  public List<FileInfo> findListByParam(FileInfoQuery param) {
    return this.fileInfoMapper.selectList(param);
  }

  /** 根据条件查询列表 */
  @Override
  public Integer findCountByParam(FileInfoQuery param) {
    return this.fileInfoMapper.selectCount(param);
  }

  /** 分页查询方法 */
  @Override
  public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
    int count = this.findCountByParam(param);
    int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

    SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
    param.setSimplePage(page);
    List<FileInfo> list = this.findListByParam(param);
    PaginationResultVO<FileInfo> result =
        new PaginationResultVO(
            count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    return result;
  }

  /** 新增 */
  @Override
  public Integer add(FileInfo bean) {
    return this.fileInfoMapper.insert(bean);
  }

  /** 批量新增 */
  @Override
  public Integer addBatch(List<FileInfo> listBean) {
    if (listBean == null || listBean.isEmpty()) {
      return 0;
    }
    return this.fileInfoMapper.insertBatch(listBean);
  }

  /** 批量新增或者修改 */
  @Override
  public Integer addOrUpdateBatch(List<FileInfo> listBean) {
    if (listBean == null || listBean.isEmpty()) {
      return 0;
    }
    return this.fileInfoMapper.insertOrUpdateBatch(listBean);
  }

  /** 根据FileIdAndUserId获取对象 */
  @Override
  public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
    return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
  }

  /** 根据FileIdAndUserId修改 */
  @Override
  public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
    return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
  }

  /** 根据FileIdAndUserId删除 */
  @Override
  public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
    return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
  }
}
