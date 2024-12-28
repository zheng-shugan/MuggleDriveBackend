package com.muggle.service.impl;

import com.muggle.component.RedisComponent;
import com.muggle.entity.config.AppConfig;
import com.muggle.entity.constants.Constants;
import com.muggle.entity.dto.SessionWebUserDto;
import com.muggle.entity.dto.UploadResultDto;
import com.muggle.entity.dto.UserSpaceDto;
import com.muggle.entity.enums.*;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.po.UserInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.entity.query.SimplePage;
import com.muggle.entity.query.UserInfoQuery;
import com.muggle.entity.vo.PaginationResultVO;
import com.muggle.exception.BusinessException;
import com.muggle.mappers.FileInfoMapper;
import com.muggle.mappers.UserInfoMapper;
import com.muggle.service.FileService;
import com.muggle.utils.StringTools;
import java.io.File;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component("fileService")
public class FileServiceImpl implements FileService {

  @Resource private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

  @Resource private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

  @Resource private RedisComponent redisComponent;

  @Resource private AppConfig appConfig;

  private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

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

  /**
   * 上传文件
   *
   * @param webUserDto
   * @param fileId
   * @param file
   * @param fileName
   * @param filePid
   * @param fileMd5
   * @param chunkIndex
   * @param chunks
   * @return
   */
  @Override
  @Transactional(rollbackFor = Exception.class)
  public UploadResultDto uploadFile(
      SessionWebUserDto webUserDto,
      String fileId,
      MultipartFile file,
      String fileName,
      String filePid,
      String fileMd5,
      Integer chunkIndex,
      Integer chunks) {

    UploadResultDto resultDto = new UploadResultDto();

    try {
      if (StringTools.isEmpty(fileId)) {
        fileId = StringTools.getRandomNumber(Constants.LENGTH_10);
      }

      resultDto.setFileId(fileId);
      Date currDate = new Date();
      UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(webUserDto.getUserId());

      if (chunkIndex == 0) {
        FileInfoQuery infoQuery = new FileInfoQuery();
        infoQuery.setFileMd5(fileMd5);
        infoQuery.setSimplePage(new SimplePage(0, 1));
        infoQuery.setStatus(FileStatusEnums.USING.getStatus());
        List<FileInfo> dbFileList = this.fileInfoMapper.selectList(infoQuery);
        // 秒传
        if (!dbFileList.isEmpty()) {
          FileInfo dbFile = dbFileList.get(0);
          // 判断文件状态
          if (dbFile.getFileSize() + userSpaceDto.getUseSpace() > userSpaceDto.getTotalSpace()) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
          }
          dbFile.setFileId(fileId);
          dbFile.setFilePid(filePid);
          dbFile.setUserId(webUserDto.getUserId());
          dbFile.setFileMd5(null);
          dbFile.setCreateTime(currDate);
          dbFile.setLastUpdateTime(currDate);
          dbFile.setStatus(FileStatusEnums.USING.getStatus());
          dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
          dbFile.setFileMd5(fileMd5);
          fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
          dbFile.setFileName(fileName);
          this.fileInfoMapper.insert(dbFile);
          resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
          // 更新用户空间使用
          updateUserSpace(webUserDto, dbFile.getFileSize());

          return resultDto;
        }
      }
      // 正常的分片上传逻辑
      Long currentTempSFileSize = redisComponent.getTempFileSize(webUserDto.getUserId(), fileId);
      // 判断文件大小
      if (file.getSize() + currentTempSFileSize + userSpaceDto.getUseSpace()
          > userSpaceDto.getTotalSpace()) {
        throw new BusinessException(ResponseCodeEnum.CODE_904);
      }
      // 临时文件目录
      String tempFilePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
      String currUserFolderPath = webUserDto.getUserId() + fileId;

      File tempFileFolder = new File(tempFilePath + currUserFolderPath);
      if (!tempFileFolder.exists()) {
        tempFileFolder.mkdirs();
      }
      File newFile = new File(tempFileFolder.getPath() + File.separator + chunkIndex);
      file.transferTo(newFile);

      if (chunkIndex < chunks - 1) {
        resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
        redisComponent.saveTempFileSize(webUserDto.getUserId(), fileId, file.getSize());
        return resultDto;
      }
    } catch (Exception e) {
      logger.error("上传文件失败", e);
    }

    return resultDto;
  }

  /**
   * 文件重命名
   *
   * @param filePid 文件父ID
   * @param userId 用户ID
   * @param fileName 文件名
   * @return
   */
  private String autoRename(String filePid, String userId, String fileName) {
    FileInfoQuery fileInfoQuery = new FileInfoQuery();
    fileInfoQuery.setUserId(userId);
    fileInfoQuery.setFilePid(filePid);
    fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
    fileInfoQuery.setFileName(fileName);
    // 如果有同名文件了，重命名
    Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
    if (count > 0) {
      return StringTools.rename(fileName);
    }

    return fileName;
  }

  /**
   * 更新用户空间
   *
   * @param webUserDto 用户信息
   * @param useSpace 文件大小
   */
  private void updateUserSpace(SessionWebUserDto webUserDto, Long useSpace) {
    Integer count = userInfoMapper.updateUserSpace(webUserDto.getUserId(), useSpace, null);
    if (count == 0) {
      throw new BusinessException(ResponseCodeEnum.CODE_904);
    }
    UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(webUserDto.getUserId());
    userSpaceDto.setUseSpace(userSpaceDto.getUseSpace() + useSpace);
    redisComponent.saveUserSpaceUse(webUserDto.getUserId(), userSpaceDto);
  }
}
