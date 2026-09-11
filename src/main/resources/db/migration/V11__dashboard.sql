alter table submission
 add ai_tools json null comment 'NULL 未填报，[] 明确没有；跟随版本保存',
 add tech_stacks json null comment 'NULL 未填报，[] 明确没有；跟随版本保存';
create table dashboard_config (
 event_id int primary key,
 public_metrics json not null,
 version int not null default 1,
 update_time datetime not null,
 foreign key(event_id) references event(event_id)
);
create index registration_event_team_index on registration(event_id, team_id);
create index team_event_track_index on team(event_id, track_id);
create index review_assignment_source_index on review_assignment(source_assignment_id);
