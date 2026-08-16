create table event
(
    event_id     int auto_increment
        primary key,
    name         varchar(50)   not null,
    reg_beg      datetime      not null,
    reg_end      datetime      not null,
    live_beg     datetime      not null,
    live_end     datetime      not null,
    introduction varchar(100)  null,
    tags         varchar(255)  null,
    notice       text          null,
    version      int default 1 not null,
    create_time  datetime      not null,
    update_time  datetime      not null,
    constraint event_pk
        unique (name)
);

create table student_tag
(
    id   int auto_increment
        primary key,
    name varchar(20) not null
);

create table track
(
    track_id    int auto_increment
        primary key,
    event_id    int           not null,
    name        varchar(50)   not null,
    desc_md     text          not null,
    version     int default 1 not null,
    create_time datetime      not null,
    update_time datetime      not null,
    constraint track_pk
        unique (event_id, name),
    constraint track_event_event_id_fk
        foreign key (event_id) references event (event_id)
);

create table phase
(
    phase_id          int auto_increment
        primary key,
    track_id          int           not null,
    name              varchar(255)  not null,
    submit_beg        datetime      not null,
    submit_end        datetime      not null,
    review_beg        datetime      not null,
    review_end        datetime      not null,
    blind_review      tinyint(1)    not null,
    mid_check         tinyint(1)    not null,
    manual_pick       tinyint(1)    not null,
    pass_rate         decimal(4, 3) not null,
    poll              tinyint(1)    not null,
    submission_config json          not null,
    version           int default 1 not null,
    create_time       datetime      not null,
    update_time       datetime      not null,
    constraint phase_pk
        unique (track_id, name),
    constraint phase_track_track_id_fk
        foreign key (track_id) references track (track_id)
);

create table user
(
    user_id      int auto_increment
        primary key,
    name         varchar(50) not null,
    password     varchar(70) null,
    student_flag tinyint     not null,
    phone        varchar(11) not null,
    email        varchar(50) not null,
    create_time  datetime    not null,
    update_time  datetime    not null,
    constraint user_pk
        unique (phone),
    constraint user_pk_2
        unique (email)
);

create table authority
(
    id          int auto_increment
        primary key,
    user_id     int      not null,
    type        tinyint  not null comment '0超管SUPER，1赛管ADMIN，2评委JUDGE',
    event_id    int      null,
    create_time datetime not null,
    constraint authority_uk
        unique (user_id, type, event_id),
    constraint authority_event_event_id_fk
        foreign key (event_id) references event (event_id),
    constraint authority_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create table ex_user
(
    user_id      int         not null
        primary key,
    on_campus    tinyint(1)  not null,
    organization varchar(50) null,
    create_time  datetime    not null,
    update_time  datetime    not null,
    constraint ex_user_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create table student
(
    user_id      int          not null
        primary key,
    cas_id       varchar(12)  not null,
    campus       varchar(5)   not null,
    major        varchar(50)  not null,
    introduction varchar(50)  null,
    tags         varchar(100) null,
    create_time  datetime     not null,
    update_time  datetime     not null,
    constraint student_pk
        unique (cas_id),
    constraint student__fk_id
        foreign key (user_id) references user (user_id)
);

create table team
(
    team_id          int auto_increment
        primary key,
    name             varchar(50)   not null,
    event_id         int           not null,
    track_id         int           not null,
    leader_id        int           not null,
    size             int           not null,
    introduction     varchar(100)  null,
    type             tinyint       not null comment '0同专业，1跨专业，2跨校区',
    status           tinyint       not null comment '0晋级，1淘汰（暂定）',
    version          int default 1 not null,
    create_time      datetime      not null,
    update_time      datetime      not null,
    current_phase_id int           null,
    constraint team_pk
        unique (name, track_id),
    constraint team_event_event_id_fk
        foreign key (event_id) references event (event_id),
    constraint team_track_track_id_fk
        foreign key (track_id) references track (track_id),
    constraint team_user_user_id_fk
        foreign key (leader_id) references user (user_id)
);

create table registration
(
    id          int auto_increment
        primary key,
    user_id     int           not null,
    event_id    int           not null,
    track_id    int           not null,
    team_id     int           null,
    version     int default 1 not null,
    create_time datetime      not null,
    update_time datetime      not null,
    constraint registration_pk
        unique (user_id, event_id),
    constraint registration_event_event_id_fk
        foreign key (event_id) references event (event_id),
    constraint registration_team_team_id_fk
        foreign key (team_id) references team (team_id),
    constraint registration_track_track_id_fk
        foreign key (track_id) references track (track_id),
    constraint registration_user_user_id_fk
        foreign key (user_id) references user (user_id)
);


