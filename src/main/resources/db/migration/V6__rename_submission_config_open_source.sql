-- submission_config 的开源提交项键名由 OpenSource 统一为 openSource（MySQL JSON 路径区分大小写）
-- 不改写存量行会导致反序列化取不到值而静默回落为 false，等于关掉该提交项
update phase
set submission_config = json_remove(
        json_set(submission_config, '$.openSource', json_extract(submission_config, '$.OpenSource')),
        '$.OpenSource')
where json_contains_path(submission_config, 'one', '$.OpenSource');
