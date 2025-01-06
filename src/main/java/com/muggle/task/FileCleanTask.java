package com.muggle.task;

import com.muggle.entity.enums.FileDelFlagEnums;
import com.muggle.entity.po.FileInfo;
import com.muggle.entity.query.FileInfoQuery;
import com.muggle.service.FileService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文件清理任务
 *
 * <p>定时清理回收站中的文件
 */
@Component
public class FileCleanTask {

  @Resource private FileService fileInfoService;

  @Scheduled(fixedDelay = 1000 * 60 * 3)
  public void execute() {
    FileInfoQuery fileInfoQuery = new FileInfoQuery();
    fileInfoQuery.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
    fileInfoQuery.setQueryExpire(true);

    List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);

    // 根据用户id 分组
    Map<String, List<FileInfo>> fileInfoMap =
        fileInfoList.stream().collect(Collectors.groupingBy(FileInfo::getUserId));

    for (Map.Entry<String, List<FileInfo>> entry : fileInfoMap.entrySet()) {
      List<String> fileIds =
          entry.getValue().stream().map(p -> p.getFileId()).collect(Collectors.toList());
      // 删除文件
      fileInfoService.delFileBatch(entry.getKey(), String.join(",", fileIds), false);
    }
  }
}
