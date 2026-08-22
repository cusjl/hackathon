create table vote
(
    id            bigint auto_increment
        primary key,
    user_id       int      not null comment '投票人，须为CAS登录用户',
    phase_id      int      not null comment '投票所在轮次',
    submission_id int      not null comment '投票对象作品',
    vote_date     date     not null comment '投票自然日，按此列计算每日票数上限',
    create_time   datetime not null,
    update_time   datetime not null,
    constraint vote_pk
        unique (user_id, submission_id),
    constraint vote_phase_phase_id_fk
        foreign key (phase_id) references phase (phase_id),
    constraint vote_submission_submission_id_fk
        foreign key (submission_id) references submission (submission_id),
    constraint vote_user_user_id_fk
        foreign key (user_id) references user (user_id)
);

create index vote_user_day_index on vote (user_id, phase_id, vote_date);
create index vote_phase_index on vote (phase_id, submission_id);
