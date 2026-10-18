ALTER TABLE cqt_school_import_tasks ADD COLUMN error_message varchar(2000) DEFAULT NULL COMMENT '任务级失败说明，不虚构Excel源行';
