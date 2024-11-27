package com.ly.ping.init;

import com.ly.commons.filelock.CommonFileLock;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class FileLockInit {
    @PostConstruct
    public void init(){
        CommonFileLock fileLock = new CommonFileLock();
        fileLock.initLockFile();
    }
}
