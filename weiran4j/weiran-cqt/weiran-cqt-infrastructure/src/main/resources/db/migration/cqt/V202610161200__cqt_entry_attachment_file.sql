-- 学校批量导入将 ZIP 内单个附件抽取为学校账号独立对象，并以 FILE 类型关联作品。
ALTER TABLE `cqt_entries`
  MODIFY COLUMN `attachment_type` enum('ZIP','LINK','FILE') DEFAULT NULL COMMENT '附件类型';
