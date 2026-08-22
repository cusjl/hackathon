package org.hackathon.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.PageParamDTO;
import org.hackathon.data.dto.ReadNoticeDTO;
import org.hackathon.data.vo.NotificationVO;
import org.hackathon.data.vo.Result;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 本人通知列表，未读优先
     * @param param 分页参数
     * @param unread 仅看未读
     * @return 分页列表
     */
    @GetMapping
    @Require
    public ResponseEntity<Result<IPage<NotificationVO>>> getNotificationPage(
            @Valid PageParamDTO param,
            @RequestParam(defaultValue = "false") Boolean unread, Context ctx) {
        return Result.success(
                notificationService.getNotificationPage(param, unread, ctx.userId()), "获取成功");
    }

    /**
     * 本人未读通知数
     * @return 未读数
     */
    @GetMapping("/unread")
    @Require
    public ResponseEntity<Result<Long>> countUnread(Context ctx) {
        return Result.success(notificationService.countUnread(ctx.userId()), "获取成功");
    }

    /**
     * 标记通知已读，不传 id 时标记全部
     * @param dto 通知id列表
     * @return ok
     */
    @PutMapping("/read")
    @Require
    public ResponseEntity<Result<Void>> read(@RequestBody(required = false) ReadNoticeDTO dto,
                                             Context ctx) {
        notificationService.read(dto, ctx.userId());
        return Result.ok();
    }
}
