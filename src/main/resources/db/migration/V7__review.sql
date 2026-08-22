create table review_dimension
(
    dimension_id int auto_increment
        primary key,
    phase_id     int              not null,
    name         varchar(50)      not null comment '维度名称，如创新性、完成度',
    description  varchar(200)     null comment '评分说明，供评委参考',
    max_score    decimal(5, 2)    not null comment '该维度满分',
    weight       decimal(5, 2)    not null comment '权重，评委总分按权重归一到百分制',
    sort_no      int    default 0 not null comment '展示顺序',
    version      int    default 1 not null,
    create_time  datetime         not null,
    update_time  datetime         not null,
    constraint review_dimension_pk
        unique (phase_id, name),
    constraint review_dimension_phase_phase_id_fk
        foreign key (phase_id) references phase (phase_id)
);

create table review_assignment
(
    assignment_id        int auto_increment
        primary key,
    phase_id             int           not null,
    submission_id        int           not null,
    judge_id             int           not null,
    status               tinyint       not null comment '0待评审，1已完成，2已回避，3已移交',
    source               tinyint       not null comment '0管理员指派，1回避后重新分发，2逾期移交',
    assigner_id          int           null comment '指派人，系统自动分发时为空',
    source_assignment_id int           null comment '由哪条任务回避/移交衍生而来',
    total_score          decimal(6, 2) null comment '本评委加权总分，百分制',
    comment              varchar(1000) null comment '本评委总评语',
    submit_time          datetime      null comment '打分提交时间',
    recuse_reason        varchar(500)  null comment '回避理由',
    recuse_time          datetime      null,
    urge_count           int default 0 not null comment '累计被催办次数',
    last_urge_time       datetime      null,
    version              int default 1 not null,
    create_time          datetime      not null,
    update_time          datetime      not null,
    constraint review_assignment_pk
        unique (submission_id, judge_id),
    constraint review_assignment_phase_phase_id_fk
        foreign key (phase_id) references phase (phase_id),
    constraint review_assignment_submission_submission_id_fk
        foreign key (submission_id) references submission (submission_id),
    constraint review_assignment_judge_user_id_fk
        foreign key (judge_id) references user (user_id)
);

create index review_assignment_judge_index on review_assignment (judge_id, status);
create index review_assignment_phase_index on review_assignment (phase_id, status);

create table review_score
(
    score_id      bigint auto_increment
        primary key,
    assignment_id int           not null,
    dimension_id  int           not null,
    score         decimal(5, 2) not null comment '该维度得分',
    comment       varchar(500)  null comment '该维度评语',
    create_time   datetime      not null,
    update_time   datetime      not null,
    constraint review_score_pk
        unique (assignment_id, dimension_id),
    constraint review_score_assignment_assignment_id_fk
        foreign key (assignment_id) references review_assignment (assignment_id),
    constraint review_score_dimension_dimension_id_fk
        foreign key (dimension_id) references review_dimension (dimension_id)
);

create table review_flag
(
    flag_id             int auto_increment
        primary key,
    submission_id       int          not null,
    phase_id            int          not null,
    judge_id            int          not null comment '发起标记的评委',
    target              tinyint      not null comment '0在线Demo，1演示视频，2演示文档，3源码压缩包，4其他',
    description         varchar(500) not null comment '异常信息描述',
    status              tinyint      not null comment '0待补交，1已补交，2已关闭',
    supplement_end      datetime     null comment '补交窗口截止时间',
    supplement_opener   int          null comment '开窗人，评委标记时即为该评委',
    supplement_open_time datetime    null,
    resolved_version_no int          null comment '补交产生的作品版本号',
    resolve_time        datetime     null,
    close_reason        varchar(200) null comment '管理员关闭窗口的理由',
    version             int default 1 not null,
    create_time         datetime     not null,
    update_time         datetime     not null,
    constraint review_flag_submission_submission_id_fk
        foreign key (submission_id) references submission (submission_id),
    constraint review_flag_phase_phase_id_fk
        foreign key (phase_id) references phase (phase_id),
    constraint review_flag_judge_user_id_fk
        foreign key (judge_id) references user (user_id)
);

create index review_flag_submission_index on review_flag (submission_id, status);
create index review_flag_window_index on review_flag (status, supplement_end);

create table notification
(
    notification_id bigint auto_increment
        primary key,
    user_id         int           not null comment '接收人',
    type            tinyint       not null comment '0评审任务指派，1任务移交，2打分催办，3Demo异常，4补交窗口关闭',
    title           varchar(100)  not null,
    content         varchar(1000) not null,
    event_id        int           null,
    phase_id        int           null,
    submission_id   int           null,
    ref_id          int           null comment '关联业务id，如 flag_id / assignment_id',
    read_flag       tinyint(1) default 0 not null,
    create_time     datetime      not null,
    constraint notification_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create index notification_receiver_index on notification (user_id, read_flag, create_time);
