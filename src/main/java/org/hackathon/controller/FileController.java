package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.UploadDTO;
import org.hackathon.data.vo.FileInfoVO;
import org.hackathon.data.vo.FileUrlVO;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.UploadVO;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.hackathon.data.enums.FileScope.Group.*;
import static org.hackathon.security.Role.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @PostMapping("/avatar")
    @Require
    public ResponseEntity<Result<UploadVO>> uploadAvatar(@Valid @RequestBody UploadDTO dto, Context ctx) {
        return Result.success(fileService.presign(dto, AVATAR, ctx), "已签发上传凭证");
    }

    @PostMapping("/event/{eventId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<UploadVO>> uploadEventAsset(@Valid @RequestBody UploadDTO dto, Context ctx) {
        return Result.success(fileService.presign(dto, EVENT_ASSET, ctx), "已签发上传凭证");
    }

    @PostMapping("/track/{trackId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<UploadVO>> uploadTrackAttachment(@Valid @RequestBody UploadDTO dto, Context ctx) {
        return Result.success(fileService.presign(dto, TRACK_ASSET, ctx), "已签发上传凭证");
    }

    @PostMapping("/phase/{phaseId}/team/{teamId}/submit")
    @Require(value = {TEAM_LEADER, TEAM_MEMBER}, window = Require.Window.SUBMIT)
    public ResponseEntity<Result<UploadVO>> uploadSubmission(@Valid @RequestBody UploadDTO dto, Context ctx) {
        return Result.success(fileService.presign(dto, SUBMIT, ctx), "已签发上传凭证");
    }

    @PostMapping("/phase/{phaseId}/team/{teamId}/appeal")
    @Require(value = TEAM_LEADER, window = Require.Window.PUBLICITY)
    public ResponseEntity<Result<UploadVO>> uploadAppeal(@Valid @RequestBody UploadDTO dto, Context ctx) {
        return Result.success(fileService.presign(dto, APPEAL, ctx), "已签发上传凭证");
    }

    @PostMapping("/team/{teamId}/showcase")
    @Require({TEAM_LEADER, EVENT_ADMIN})
    public ResponseEntity<Result<UploadVO>> uploadShowcase(@Valid @RequestBody UploadDTO dto, Context ctx) {
        return Result.success(fileService.presign(dto, SHOWCASE, ctx), "已签发上传凭证");
    }

    @PostMapping("/{fileId}/complete")
    @Require
    public ResponseEntity<Result<FileInfoVO>> complete(@PathVariable Long fileId, Context ctx) {
        return Result.success(fileService.complete(fileId, ctx), "上传完成");
    }

    @GetMapping("/{fileId}/url")
    public ResponseEntity<Result<FileUrlVO>> url(@PathVariable Long fileId,
                                                 @RequestParam(defaultValue = "false") boolean download,
                                                 Context ctx) {
        return Result.success(fileService.url(fileId, download, ctx), "获取成功");
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Result<FileInfoVO>> info(@PathVariable Long fileId, Context ctx) {
        return Result.success(fileService.info(fileId, ctx), "获取成功");
    }

    @DeleteMapping("/{fileId}")
    @Require
    public ResponseEntity<Result<Void>> delete(@PathVariable Long fileId, Context ctx) {
        fileService.delete(fileId, ctx);
        return Result.ok();
    }
}
