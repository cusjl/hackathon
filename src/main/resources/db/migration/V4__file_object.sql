create table file_object
(
    file_id      bigint auto_increment
        primary key,
    object_key   varchar(255) not null comment '对象存储 key，服务端生成',
    scope        tinyint      not null comment '用途，见 FileScope',
    status       tinyint      not null comment '0 待上传 1 可用 2 已删除',
    origin_name  varchar(200) not null comment '原始文件名，仅用于展示与下载命名',
    content_type varchar(100) not null,
    size_bytes   bigint       not null comment 'complete 后为 HeadObject 复核的真实值',
    etag         varchar(64)  null,
    uploader_id  int          not null,
    user_id      int          null comment '锚点：头像 / 证书归属人',
    event_id     int          null,
    track_id     int          null,
    phase_id     int          null,
    team_id      int          null,
    create_time  datetime     not null,
    update_time  datetime     not null,
    constraint file_object_pk
        unique (object_key),
    constraint file_object_uploader_user_id_fk
        foreign key (uploader_id) references user (user_id),
    constraint file_object_owner_user_id_fk
        foreign key (user_id) references user (user_id),
    constraint file_object_event_event_id_fk
        foreign key (event_id) references event (event_id),
    constraint file_object_track_track_id_fk
        foreign key (track_id) references track (track_id),
    constraint file_object_phase_phase_id_fk
        foreign key (phase_id) references phase (phase_id),
    constraint file_object_team_team_id_fk
        foreign key (team_id) references team (team_id)
);

create index file_object_anchor_index on file_object (scope, team_id, phase_id);
create index file_object_status_create_index on file_object (status, create_time);
create index file_object_status_update_index on file_object (status, update_time);
create index file_object_uploader_index on file_object (uploader_id);
