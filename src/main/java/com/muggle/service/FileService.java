package com.muggle.service;

import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.dto.UploadResultDto;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.vo.PaginationResultVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

  /** 根据条件查询列表 */
  List<FileInfo> findListByParam(FileInfoQuery param);

  /** 根据条件查询列表 */
  Integer findCountByParam(FileInfoQuery param);

  /** 分页查询 */
  PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param);

  /** 新增 */
  Integer add(FileInfo bean);

  /** 批量新增 */
  Integer addBatch(List<FileInfo> listBean);

  /** 批量新增/修改 */
  Integer addOrUpdateBatch(List<FileInfo> listBean);

  /** 根据FileIdAndUserId查询对象 */
  FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId);

  /** 根据FileIdAndUserId修改 */
  Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId);

  /** 根据FileIdAndUserId删除 */
  Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId);

  UploadResultDto uploadFile(
      SessionWebUserDto webUserDto,
      String fileId,
      MultipartFile file,
      String fileName,
      String filePid,
      String fileMd5,
      Integer chunkIndex,
      Integer chunks);

  FileInfo newFolder(String userId, String filePid, String folderName);

  FileInfo rename(String userId, String filePid, String fileId, String fileName);

  public void changeFileFolder(String fileIds, String filePid, String userId);

  public void removeFile2RecycleBatch(String userId, String fileIds);

  public void recoverFileBatch(String userId, String fileIds);
}
