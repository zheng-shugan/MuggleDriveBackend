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
import com.muggle.utils.DateUtil;
import com.muggle.utils.ProcessUtils;
import com.muggle.utils.ScaleFilter;
import com.muggle.utils.StringTools;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Component("fileService")
public class FileServiceImpl implements FileService {

  @Resource private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

  @Resource private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

  @Resource private RedisComponent redisComponent;

  @Resource private AppConfig appConfig;

  private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

  @Resource @Lazy private FileServiceImpl fileService;

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
    Boolean uploadSuccess = true;
    File tempFileFolder = null;

    try {
      if (StringTools.isEmpty(fileId)) {
        fileId = StringTools.getRandomNumber(Constants.LENGTH_10);
      }

      resultDto.setFileId(fileId);
      Date currDate = new Date();
      UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(webUserDto.getUserId());

      // 文件秒传
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

      tempFileFolder = new File(tempFilePath + currUserFolderPath);
      if (!tempFileFolder.exists()) {
        tempFileFolder.mkdirs();
      }
      File newFile = new File(tempFileFolder.getPath() + File.separator + chunkIndex);
      file.transferTo(newFile);

      // 最后一个分片
      if (chunkIndex < chunks - 1) {
        resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
        redisComponent.saveTempFileSize(webUserDto.getUserId(), fileId, file.getSize());
        return resultDto;
      }
      // 不是最后一个分片也要把文件大小加上
      redisComponent.saveTempFileSize(webUserDto.getUserId(), fileId, file.getSize());

      // 最后一个分片上传完，异步合并分片
      String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
      String fileSuffix = StringTools.getFileSuffix(fileName);
      String newFileName = currUserFolderPath + fileSuffix;
      FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
      fileName = autoRename(filePid, webUserDto.getUserId(), fileName);
      // 新建文件
      FileInfo fileInfo = new FileInfo();
      fileInfo.setFileId(fileId);
      fileInfo.setUserId(webUserDto.getUserId());
      fileInfo.setFileMd5(fileMd5);
      fileInfo.setFilePid(filePid);
      fileInfo.setFileName(fileName);
      fileInfo.setFilePath(month + File.separator + newFileName);
      fileInfo.setCreateTime(currDate);
      fileInfo.setLastUpdateTime(currDate);
      fileInfo.setFileCategory(fileTypeEnums.getCategory().getCategory());
      fileInfo.setFileType(fileTypeEnums.getType());
      fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
      fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
      fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
      fileInfoMapper.insert(fileInfo);
      // 更新用户的使用空间
      Long totalSize = redisComponent.getTempFileSize(webUserDto.getUserId(), fileId);
      updateUserSpace(webUserDto, totalSize);
      // 更新上传状态
      resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
      // 文件转码，要在上一个事务完成后执行
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              fileService.transferFile(webUserDto, fileInfo.getFileId());
            }
          });

      return resultDto;

    } catch (BusinessException e) {
      logger.error("上传文件失败", e);
      uploadSuccess = false;
      throw e;
    } catch (Exception e) {
      logger.error("上传文件失败", e);
      uploadSuccess = false;
    } finally {
      if (uploadSuccess == false && tempFileFolder != null) {
        try {
          FileUtils.deleteDirectory(tempFileFolder);
        } catch (IOException e) {
          logger.error("删除临时目录失败", e);
        }
      }
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
    // 更新用户空间
    Integer count = userInfoMapper.updateUserSpace(webUserDto.getUserId(), useSpace, null);
    if (count == 0) {
      throw new BusinessException(ResponseCodeEnum.CODE_904);
    }
    UserSpaceDto userSpaceDto = redisComponent.getUserSpaceDto(webUserDto.getUserId());
    userSpaceDto.setUseSpace(userSpaceDto.getUseSpace() + useSpace);
    redisComponent.saveUserSpaceUse(webUserDto.getUserId(), userSpaceDto);
  }

  @Async
  public void transferFile(SessionWebUserDto userDto, String fileId) {
    Boolean transferSuccess = true;
    String targetFilePath = null;
    FileTypeEnums fileTypeEnums = null;
    FileInfo fileInfo = fileInfoMapper.selectByFileIdAndUserId(fileId, userDto.getUserId());
    String coverImagePath = null;

    try {
      // 文件不存在或者文件状态不是转码状态
      if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
        return;
      }
      // 获取临时目录
      String tempFileName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
      String currUserFolderName = userDto.getUserId() + fileInfo.getFileId();
      File fileFolder = new File(tempFileName + currUserFolderName);
      if (!fileFolder.exists()) {
        return;
      }
      String fileSuffix = StringTools.getFileSuffix(fileInfo.getFileName());
      String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
      // 目标目录
      String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
      File targetFolder = new File(targetFolderName + month);
      if (!targetFolder.exists()) {
        targetFolder.mkdir();
      }
      // 目标文件
      String newFileName = currUserFolderName + fileSuffix;
      // 真正的文件路径
      targetFilePath = targetFolder.getPath() + File.separator + newFileName;

      // 合并文件
      unionFile(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);

      // 视频文件分片
      fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
      if (fileTypeEnums == FileTypeEnums.VIDEO) {
        // 视频分片
        splitFile4Video(fileId, targetFilePath);
        // 缩略图
        coverImagePath = month + File.separator + currUserFolderName + Constants.IMAGE_PNG_SUFFIX;
        String coverPath = targetFolderName + File.separator + coverImagePath;
        ScaleFilter.createCover4Video(
            new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
      } else if (fileTypeEnums == FileTypeEnums.IMAGE) {
        // 图片的缩略图
        coverImagePath = month + File.separator + newFileName.replace(".", "_.");
        String coverPath = targetFolderName + File.separator + coverImagePath;
        Boolean aBoolean =
            ScaleFilter.createThumbnailWidthFFmpeg(
                new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
        // 创建缩略图失败，直接复制文件
        if (!aBoolean) {
          FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
        }
      }

    } catch (Exception e) {
      logger.error("文件转码失败，文件id:{}, 用户id:{}", fileId, userDto.getUserId(), e);
      transferSuccess = false;
      throw new BusinessException("文件转码失败");
    } finally {
      FileInfo updateFileInfo = new FileInfo();
      updateFileInfo.setFileSize(new File(targetFilePath).length());
      updateFileInfo.setFileCover(coverImagePath);
      updateFileInfo.setStatus(
          transferSuccess
              ? FileStatusEnums.USING.getStatus()
              : FileStatusEnums.TRANSFER_FAIL.getStatus());
      fileInfoMapper.updateFileStatusWithOldStatus(
          fileId, userDto.getUserId(), updateFileInfo, FileStatusEnums.TRANSFER.getStatus());
    }
  }

  /**
   * 合并文件
   *
   * @param dirPath
   * @param targetFilePath
   * @param fileName
   * @param delSource
   */
  private void unionFile(
      String dirPath, String targetFilePath, String fileName, Boolean delSource) {
    File dir = new File(dirPath);
    if (!dir.exists()) {
      logger.error("目录不存在");
      throw new BusinessException("目录不存在");
    }
    // 获取目录下的所有文件
    File[] files = dir.listFiles();
    File targetFile = new File(targetFilePath);
    RandomAccessFile writeFile = null;
    try {
      writeFile = new RandomAccessFile(targetFile, "rw");
      byte[] bytes = new byte[1024 * 10];
      for (int i = 0; i < files.length; i += 1) {
        int len = -1;
        File chunkFile = new File(dir + File.separator + i);
        RandomAccessFile readFile = null;
        try {
          readFile = new RandomAccessFile(chunkFile, "r");
          while ((len = readFile.read(bytes)) != -1) {
            writeFile.write(bytes, 0, len);
          }
        } catch (Exception e) {
          logger.error("合并分片失败");
          throw new BusinessException("合并分片失败");
        } finally {
          readFile.close();
        }
      }

    } catch (Exception e) {
      logger.error("合并文件:{}失败", fileName, e);
      throw new BusinessException("合并文件" + fileName + "出错");
    } finally {
      if (writeFile != null) {
        try {
          writeFile.close();
        } catch (IOException e) {
          logger.error("关闭文件流失败", e);
          throw new BusinessException("合并文件" + fileName + "出错");
        }
      }
      if (delSource && dir.exists()) {
        try {
          FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
          logger.error("删除目录失败", e);
          throw new BusinessException("合并文件" + fileName + "出错");
        }
      }
    }
  }

  /**
   * 视频文件分片
   *
   * @param fileId 文件ID
   * @param videoFilePath 视频文件路径
   */
  public void splitFile4Video(String fileId, String videoFilePath) {
    // 创建同名切片目录
    File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
    if (!tsFolder.exists()) {
      if (!tsFolder.mkdir()) {
        System.err.println("Failed to create directory: " + tsFolder.getPath());
        return;
      }
    }

    String inputFilePath = videoFilePath;
    String outputFolderPath = tsFolder.getPath();

    String command =
        String.format(
            "ffmpeg -i %s -c:v copy -c:a copy -bsf:v h264_mp4toannexb -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename %s"
                + File.separator
                + "%s_%%03d.ts %s/index.m3u8",
            inputFilePath, // 输入文件路径
            outputFolderPath, // TS 文件输出目录
            fileId, // 文件ID
            outputFolderPath // M3U8 文件输出目录
            );

    ProcessUtils.executeCommand(command, false);
  }
}
