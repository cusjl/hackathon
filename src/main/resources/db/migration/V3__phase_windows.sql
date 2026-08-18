alter table phase
    add column poll_beg       datetime null comment '大众投票开始，poll=1 时必填',
    add column poll_end       datetime null comment '大众投票结束，poll=1 时必填',
    add column poll_daily_cap int      null comment '每账号每日票数上限',
    add column publicity_end  datetime null comment '成绩公示期截止';
