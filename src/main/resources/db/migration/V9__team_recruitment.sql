alter table event
    add column team_min_size int default 1 not null after notice,
    add column team_max_size int default 5 not null after team_min_size;

alter table team
    add column invite_code varchar(16) null after current_phase_id,
    add constraint team_invite_code_uk unique (invite_code);

create table team_recruitment
(
    recruitment_id int auto_increment primary key,
    team_id        int           not null,
    title          varchar(100)  not null,
    description    varchar(1000) null,
    required_tags  varchar(500)  null,
    vacancies      int           not null,
    open_flag      tinyint(1) default 1 not null,
    version        int        default 1 not null,
    create_time    datetime      not null,
    update_time    datetime      not null,
    constraint team_recruitment_team_fk
        foreign key (team_id) references team (team_id) on delete cascade
);

create index team_recruitment_open_index on team_recruitment (open_flag, update_time);

create table team_application
(
    application_id int auto_increment primary key,
    recruitment_id int      not null,
    team_id        int      not null,
    user_id        int      not null,
    status         tinyint  not null comment '0待处理，1已接受，2已拒绝，3已取消',
    create_time    datetime not null,
    update_time    datetime not null,
    constraint team_application_uk unique (recruitment_id, user_id),
    constraint team_application_recruitment_fk
        foreign key (recruitment_id) references team_recruitment (recruitment_id) on delete cascade,
    constraint team_application_team_fk
        foreign key (team_id) references team (team_id) on delete cascade,
    constraint team_application_user_fk
        foreign key (user_id) references user (user_id)
);

create index team_application_team_status_index on team_application (team_id, status, update_time);

create table team_invitation
(
    invitation_id int auto_increment primary key,
    team_id       int      not null,
    user_id       int      not null,
    inviter_id    int      not null,
    status        tinyint  not null comment '0待处理，1已接受，2已拒绝，3已取消',
    create_time   datetime not null,
    update_time   datetime not null,
    constraint team_invitation_uk unique (team_id, user_id),
    constraint team_invitation_team_fk
        foreign key (team_id) references team (team_id) on delete cascade,
    constraint team_invitation_user_fk
        foreign key (user_id) references user (user_id),
    constraint team_invitation_inviter_fk
        foreign key (inviter_id) references user (user_id)
);

create index team_invitation_user_status_index on team_invitation (user_id, status, update_time);

create table student_recommendation
(
    recommendation_id int auto_increment primary key,
    event_id           int           not null,
    track_id           int           not null,
    user_id            int           not null,
    introduction       varchar(1000) not null,
    skills             varchar(500)  null,
    open_flag          tinyint(1) default 1 not null,
    version            int        default 1 not null,
    create_time        datetime      not null,
    update_time        datetime      not null,
    constraint student_recommendation_uk unique (event_id, user_id),
    constraint student_recommendation_event_fk
        foreign key (event_id) references event (event_id) on delete cascade,
    constraint student_recommendation_track_fk
        foreign key (track_id) references track (track_id),
    constraint student_recommendation_user_fk
        foreign key (user_id) references user (user_id)
);

create index student_recommendation_search_index on student_recommendation (event_id, track_id, open_flag, update_time);
