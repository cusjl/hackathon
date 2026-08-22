create table submission
(
    submission_id   int auto_increment
        primary key,
    phase_id        int           not null,
    team_id         int           not null,
    repo_url        varchar(255)  null comment '代码仓库地址',
    license_type    varchar(50)   null comment '开源许可协议',
    derived_from    varchar(500)  null comment '二次开发原项目说明',
    archive_file_id bigint        null comment '源码压缩包，引用 file_object.file_id',
    video_file_id   bigint        null comment '演示视频文件，引用 file_object.file_id',
    video_url       varchar(500)  null comment '演示视频链接，与视频文件二选一',
    doc_file_id     bigint        null comment '演示文档 PPT/PDF，引用 file_object.file_id',
    demo_url        varchar(500)  null comment '在线 Demo 网址',
    intro_md        text          null comment '项目详细介绍 Markdown',
    declaration     text          null comment '开源及 AI 声明',
    status          tinyint       not null comment '0 已提交，1 已提交待评审（截止锁定）',
    version_no      int           not null comment '当前版本号，v1 起递增',
    submitter_id    int           not null comment '最后一次提交人',
    submit_time     datetime      not null comment '最后一次提交时间',
    version         int default 1 not null,
    create_time     datetime      not null,
    update_time     datetime      not null,
    constraint submission_pk
        unique (phase_id, team_id),
    constraint submission_phase_phase_id_fk
        foreign key (phase_id) references phase (phase_id),
    constraint submission_team_team_id_fk
        foreign key (team_id) references team (team_id),
    constraint submission_user_user_id_fk
        foreign key (submitter_id) references user (user_id)
);

-- 三个文件列不建外键：文件由 FileCleanUpTask 定期物理清理，外键会阻塞清理任务
create index submission_team_index on submission (team_id);
create index submission_status_index on submission (status);

create table submission_version
(
    version_id    bigint auto_increment
        primary key,
    submission_id int          not null,
    version_no    int          not null comment '版本序号，v1、v2 …',
    snapshot      json         not null comment '该版本作品内容全量快照',
    change_log    varchar(200) null comment '本次提交说明',
    submitter_id  int          not null,
    submit_time   datetime     not null,
    create_time   datetime     not null,
    constraint submission_version_pk
        unique (submission_id, version_no),
    constraint submission_version_submission_id_fk
        foreign key (submission_id) references submission (submission_id),
    constraint submission_version_user_user_id_fk
        foreign key (submitter_id) references user (user_id)
);
