ALTER TABLE cqt_resources
    CHANGE COLUMN file_url material_url varchar(1024) NOT NULL DEFAULT '',
    DROP COLUMN file_name;
