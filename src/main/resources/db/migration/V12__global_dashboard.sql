-- 全局公开配置独立于旧赛事配置，不扩大旧配置的公开范围。
-- dashboard_config 继续服务赛事级接口；全局接口使用下面的单例配置。
create table global_dashboard_config (
 config_id int primary key,
 public_metrics json not null,
 version int not null default 0,
 update_time datetime null,
 constraint global_dashboard_config_singleton check (config_id = 1)
);
insert into global_dashboard_config(config_id, public_metrics, version, update_time)
values (1, JSON_ARRAY(), 0, null);
