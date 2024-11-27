package com.ly.commons.filelock;

import com.ly.commons.constants.CommonConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

/**
 * 该对象适用于单机环境下的多应用竞争资源使用
 */
@Component
@Log4j2
public class CommonFileLock {
    private KafkaTemplate kafkaTemplate;

    /**
     * 获取客户端锁，获取锁成功，则可以正常提交请求
     *
     * @return true:成功获取到锁信息（此时响应的信息已经写入到文件锁中）
     * false：未获取到锁（说明客户端不能提交）
     * null: 需要限流
     */
    public Boolean getRateLimitLock() {
        FileLock lock = null;
        File lockFile = new File(getFilePath());
        int getLockTimes;
        if (!lockFile.exists()) {
            log.error("文件锁不存在");
            throw new RuntimeException("文件锁不存在");
        }
        Long currentTimestamp = null;

        try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
             FileChannel fileChannel = raf.getChannel()) {
            getLockTimes = 0;
            while (lock == null && ((getLockTimes++) < CommonConstants.LOCK_RETRY_TIMES)) {
                lock = fileChannel.lock();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.info("第" + getLockTimes + "次重新获取锁");
                }
            }
            if (lock != null) {
                String fileContent = readLockFileContent(raf);
                log.info("读取到的文件内容" + fileContent);
                Long oldestTimestamp = parseOldestTimeFromFile(fileContent);
                currentTimestamp = System.currentTimeMillis();
                if (currentTimestamp - oldestTimestamp > CommonConstants.RATE_LIMIT_THRESHOLD) {
                    raf.setLength(0);
                    raf.seek(0);
                    raf.write(generateNewContent(fileContent));
                    return true;
                } else {
                    lock.release();
                    log.info("时间窗口内请求达到上限，阻止当前内容的发送");
                    return null;
                }
            } else {
                log.info("未获取到文件锁");
                return false;
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return true;
    }

    /**
     * 这里的逻辑很简单
     * 未达到最大提交数量时：直接将最新时间拼接到原时间内容的最后面
     * 已达到最大提交数量时：去除第一个timestamp值，将当前时间拼接到剩余时间内容的最后面
     *
     * @param contentFromLockFile
     * @return 需要存储到文件中的时间戳信息
     */
    private byte[] generateNewContent(String contentFromLockFile) {
        long size = 0;
        if (contentFromLockFile.length() % CommonConstants.TIMESTAMP_LENGTH != 0) {
            // 如果读取的文件内容长度不是timestamp长度整数倍，说明流程有问题
            throw new RuntimeException("文件锁获取异常");
        }
        size = contentFromLockFile.length() / CommonConstants.TIMESTAMP_LENGTH;

        String currentTimestampStr = String.valueOf(System.currentTimeMillis());
        if (size == 0) {
            return currentTimestampStr.getBytes(StandardCharsets.UTF_8);
        } else if (size < CommonConstants.RATE_LIMIT_NUM) {
            // 未达到客户限速的最大提交数量时
            return (contentFromLockFile + currentTimestampStr)
                    .getBytes(StandardCharsets.UTF_8);
        } else {
            if (CommonConstants.RATE_LIMIT_NUM < 2) {
                throw new RuntimeException("配置项RATE_LIMIT_NUM需要配置为>1的正整数");
            }
            // 去除第一个timestamp值，将当前时间拼接到剩余时间内容的最后面（jdk会优化+）
            return (contentFromLockFile.substring(
                    CommonConstants.TIMESTAMP_LENGTH * (CommonConstants.RATE_LIMIT_NUM - 1),
                    contentFromLockFile.length()) + currentTimestampStr)
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 获取文件锁中，最老的时间戳，也就是文件中第一个timestamp值
     *
     * @param content 文件锁中读取到的内容
     * @return 文件中第一个timestamp值，初始化情况下，返回0
     */
    private Long parseOldestTimeFromFile(String content) {
        if (content.isEmpty()) {
            // 初始情况下
            return 0L;
        }
        try {
            return Long.parseLong(content.substring(0,
                    CommonConstants.TIMESTAMP_LENGTH));
        } catch (Exception e) {
            log.error("时间戳解析失败: " + e.getMessage());
            return 0L;
        }
    }

    private static String readLockFileContent(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        String line = raf.readLine();
        return line == null ? "" : line.trim();
    }

    /**
     * 初始化生成lock文件，兼容不同操作系统
     */
    public void initLockFile() {
        File file = new File(getFilePath());

        try {
            if (file.createNewFile()) {
                log.info("文件已创建: " + file.getAbsolutePath());
            } else {
                log.info("文件已存在: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 不同操作系统中，lock文件的路径
     *
     * @return lock文件的路径
     */
    private String getFilePath() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return CommonConstants.WINDOWS_FILE_LOCK_PATH;
        } else if (osName.contains("nix") || osName.contains("nux") ||
                osName.contains("aix")) {
            return CommonConstants.LINUX_FILE_LOCK_PATH;
        } else {
            log.warn("暂不支持的操作系统");
        }
        return CommonConstants.LINUX_FILE_LOCK_PATH;
    }
}
