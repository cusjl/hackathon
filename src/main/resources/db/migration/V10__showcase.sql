create table showcase_project (
 showcase_id int auto_increment primary key,
 team_id int not null unique,
 event_id int not null,
 track_id int not null,
 source_submission_id int not null,
 source_version_no int not null,
 title varchar(100) not null,
 summary varchar(500) null,
 intro_md mediumtext null,
 repo_url varchar(500) null,
 license_type varchar(50) null,
 derived_from varchar(500) null,
 demo_url varchar(500) null,
 video_url varchar(500) null,
 cover_file_id bigint null,
 video_file_id bigint null,
 status varchar(20) not null,
 moderation_reason varchar(500) null,
 published_at datetime null,
 version int not null default 1,
 create_time datetime not null,
 update_time datetime not null,
 foreign key (team_id) references team(team_id),
 foreign key (event_id) references event(event_id),
 foreign key (track_id) references track(track_id),
 foreign key (source_submission_id, source_version_no) references submission_version(submission_id, version_no)
);
create index showcase_public_index on showcase_project(status, event_id, track_id, published_at, showcase_id);
create table showcase_project_file (
 showcase_id int not null,
 file_id bigint not null,
 role varchar(20) not null,
 primary key(showcase_id, file_id),
 foreign key(showcase_id) references showcase_project(showcase_id),
 foreign key(file_id) references file_object(file_id)
);
create table showcase_operation_log (
 id bigint auto_increment primary key,
 showcase_id int not null,
 actor_id int not null,
 action varchar(20) not null,
 reason varchar(500) null,
 create_time datetime not null,
 foreign key(showcase_id) references showcase_project(showcase_id),
 foreign key(actor_id) references user(user_id)
);
