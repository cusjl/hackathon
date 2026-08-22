package org.hackathon.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hackathon.data.enums.SubmissionStatus;

import java.time.LocalDateTime;

/**
 * 队伍作品，一个队伍在一个轮次下有且只有一份（phase_id + team_id 唯一）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Submission {
    @TableId(type = IdType.AUTO)
    private Integer submissionId;
    private Integer phaseId;
    private Integer teamId;
    private String repoUrl;
    private String licenseType;
    private String derivedFrom;
    private Long archiveFileId;
    private Long videoFileId;
    private String videoUrl;
    private Long docFileId;
    private String demoUrl;
    private String introMd;
    private String declaration;
    private SubmissionStatus status;
    //当前版本号，v1 起递增
    private Integer versionNo;
    //最后一次提交人
    private Integer submitterId;
    private LocalDateTime submitTime;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 取出当前作品内容快照
     */
    public SubmissionSnapshot snapshot() {
        SubmissionSnapshot snapshot = new SubmissionSnapshot();
        snapshot.setRepoUrl(repoUrl);
        snapshot.setLicenseType(licenseType);
        snapshot.setDerivedFrom(derivedFrom);
        snapshot.setArchiveFileId(archiveFileId);
        snapshot.setVideoFileId(videoFileId);
        snapshot.setVideoUrl(videoUrl);
        snapshot.setDocFileId(docFileId);
        snapshot.setDemoUrl(demoUrl);
        snapshot.setIntroMd(introMd);
        snapshot.setDeclaration(declaration);
        return snapshot;
    }

    /**
     * 用校验通过的快照覆盖作品内容
     */
    public void apply(SubmissionSnapshot snapshot) {
        repoUrl = snapshot.getRepoUrl();
        licenseType = snapshot.getLicenseType();
        derivedFrom = snapshot.getDerivedFrom();
        archiveFileId = snapshot.getArchiveFileId();
        videoFileId = snapshot.getVideoFileId();
        videoUrl = snapshot.getVideoUrl();
        docFileId = snapshot.getDocFileId();
        demoUrl = snapshot.getDemoUrl();
        introMd = snapshot.getIntroMd();
        declaration = snapshot.getDeclaration();
    }

    /**
     * 按当前时间派生状态：提交截止后一律视为“已提交，待评审”。
     * 定时任务未跑到时由此兜底，保证查询结果与时间窗一致。
     * @param phase 作品所属轮次
     * @return 派生后的状态
     */
    public SubmissionStatus derivedStatus(Phase phase) {
        if (phase.getSubmitEnd() != null && LocalDateTime.now().isAfter(phase.getSubmitEnd())) {
            return SubmissionStatus.LOCKED;
        }
        return status;
    }
}
