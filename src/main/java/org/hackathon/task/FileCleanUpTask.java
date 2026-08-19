package org.hackathon.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hackathon.config.S3Properties;
import org.hackathon.data.po.FileObject;
import org.hackathon.mapper.FileObjectMapper;
import org.hackathon.service.StorageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanUpTask {

    private final FileObjectMapper fileObjectMapper;
    private final StorageService storageService;
    private final S3Properties props;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanPending() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(props.pendingExpireHours());
        List<FileObject> files = fileObjectMapper.selectTimeoutFiles(deadline);
        for (FileObject file : files) {
            storageService.delete(file.getObjectKey());
            fileObjectMapper.deleteById(file.getFileId());
        }
        if (!files.isEmpty()) {
            log.info("清理未完成上传记录 {} 条", files.size());
        }
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanDeleted() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(props.deletedRetainHours());
        List<FileObject> files = fileObjectMapper.selectDeletedFiles(deadline);
        for (FileObject file : files) {
            storageService.delete(file.getObjectKey());
            fileObjectMapper.deleteById(file.getFileId());
        }
        if (!files.isEmpty()) {
            log.info("清理已删除文件 {} 条", files.size());
        }
    }
}
