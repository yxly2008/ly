package com.ly.commons.constants;

public class CommonConstants {
    public static final String DEFAULT_TOPIC_PREFIX = "topic_ping_pong_";

    /**
     * 心跳发送间隔，以毫秒为单位
     */
    public static final int HEART_BEAT_INTERVAL = 1000;
    /**
     * 限流时间窗口
     */
    public static final int RATE_LIMIT_THRESHOLD = 1000;

    // LOG
    public static final String LOG_SENT_AND_PONG = "Request sent & Pong Respond.";
    public static final String LOG_SENT_AND_PONG_THROTTLED = "Request send &" +
            " Pong throttled it.";
    public static final String LOG_RATE_LIMITED = "Request not send as being \"rate limited\".";

    // FILE LOCK
    public static final String WINDOWS_FILE_LOCK_PATH = "C:\\Windows\\Temp" +
            "\\ping_pong.lock";
    public static final String LINUX_FILE_LOCK_PATH = "/tmp/ping_pong.lock";
    public static final int TIMESTAMP_LENGTH =
            String.valueOf(System.currentTimeMillis()).length();
    public static final int RATE_LIMIT_NUM = 2;
    public static final int LOCK_RETRY_TIMES = 3;

    // mongodb
    public static final String DEFAULT_DOCUMENT = "ping_pong";
    public static final int MONGODB_BATCH_SUBMIT_MUM = 100;
}
