package test;


import com.muggle.utils.ProcessUtils;

import java.io.File;

public class FileTest {
  private static void splitFile4Video(String fileId, String videoFilePath) {
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

    String command = String.format(
            "ffmpeg -i %s -c:v copy -c:a copy -bsf:v h264_mp4toannexb -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename %s" + File.separator + "output_%%03d.ts %s/output.m3u8",
            inputFilePath,     // 输入文件路径
            outputFolderPath,  // TS 文件输出目录
            outputFolderPath   // M3U8 文件输出目录
    );

    System.out.println(command);
    ProcessUtils.executeCommand(command, false);
  }

  public static void main(String[] args) {
    splitFile4Video("0", "/Users/zhengshugan/Downloads/unnamed/input.mp4");
  }
}
