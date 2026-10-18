-- Review and award workflow; immutable revisions and phase-locked commands.
CREATE TABLE cqt_entry_review_decisions (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
entry_id bigint NOT NULL, decision varchar(20) NOT NULL, reason varchar(2000) NOT NULL, operator_id bigint NOT NULL, version bigint NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_entry_version(entry_id,version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_phases (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
competition_id bigint NOT NULL, code varchar(20) NOT NULL, state varchar(20) NOT NULL DEFAULT 'DRAFT', current_rule_id bigint NOT NULL DEFAULT 0, input_version bigint NOT NULL DEFAULT 0, version bigint NOT NULL DEFAULT 0,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_competition_code(competition_id,code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_phase_rules (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
phase_id bigint NOT NULL, revision bigint NOT NULL, min_score decimal(8,2) NOT NULL, max_score decimal(8,2) NOT NULL, minimum_reviewers int NOT NULL, passing_score decimal(8,2) NULL, created_by bigint NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_phase_revision(phase_id,revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_dimensions (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
rule_id bigint NOT NULL, first_category_id bigint NOT NULL, name varchar(255) NOT NULL, description varchar(2000) NOT NULL DEFAULT '', min_score decimal(8,2) NOT NULL, max_score decimal(8,2) NOT NULL, sort int NOT NULL DEFAULT 0,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_rule_dimension(rule_id,first_category_id,name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_reviewer_scopes (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
user_id bigint NOT NULL, competition_id bigint NOT NULL, phase_id bigint NOT NULL, first_category_id bigint NOT NULL, second_category_id bigint NOT NULL DEFAULT 0, region_id bigint NOT NULL DEFAULT 0, enabled boolean NOT NULL, version bigint NOT NULL DEFAULT 0,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_scope(user_id,phase_id,first_category_id,second_category_id,region_id), KEY idx_user_phase(user_id,phase_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_assignments (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
phase_id bigint NOT NULL, target_id bigint NOT NULL, reviewer_id bigint NOT NULL, state varchar(20) NOT NULL DEFAULT 'ACTIVE', version bigint NOT NULL DEFAULT 0, user_state varchar(20) NOT NULL, user_updated_at datetime NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_assignment(phase_id,target_id,reviewer_id), KEY idx_reviewer(reviewer_id,state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_scores (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
assignment_id bigint NOT NULL, current_revision bigint NOT NULL DEFAULT 0, status varchar(20) NOT NULL, rule_id bigint NOT NULL, version bigint NOT NULL DEFAULT 0,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_score_assignment(assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_score_revisions (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
score_id bigint NOT NULL, revision bigint NOT NULL, total decimal(8,2) NULL, comment varchar(2000) NOT NULL, status varchar(20) NOT NULL, rule_id bigint NOT NULL, operator_id bigint NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_score_revision(score_id,revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_dimension_scores (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
score_revision_id bigint NOT NULL, dimension_id bigint NOT NULL, value decimal(8,2) NULL, comment varchar(2000) NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_revision_dimension(score_revision_id,dimension_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_phase_target_status (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
phase_id bigint NOT NULL, target_id bigint NOT NULL, decision varchar(20) NOT NULL, status_version bigint NOT NULL DEFAULT 0, reason varchar(2000) NOT NULL, input_final_version bigint NOT NULL DEFAULT 0, state varchar(20) NOT NULL DEFAULT 'VALID',
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_phase_target(phase_id,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_final_scores (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
phase_id bigint NOT NULL, target_id bigint NOT NULL, final_score decimal(8,2) NULL, average_score decimal(8,2) NULL, valid_reviewer_count int NOT NULL, state varchar(20) NOT NULL, version bigint NOT NULL, snapshot_id bigint NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_final(phase_id,target_id), KEY idx_final_state(phase_id,state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_score_snapshots (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
phase_id bigint NOT NULL, target_id bigint NOT NULL, input_version bigint NOT NULL, kind varchar(40) NOT NULL, input_json longtext NOT NULL, output_json longtext NOT NULL, reason varchar(2000) NOT NULL, operator_id bigint NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
KEY idx_snapshot_target(phase_id,target_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_score_corrections (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
final_score_id bigint NOT NULL, before_score decimal(8,2) NULL, after_score decimal(8,2) NOT NULL, reason varchar(2000) NOT NULL, operator_id bigint NOT NULL, final_version bigint NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_final_version(final_score_id,final_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_qualifications (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
source_phase_id bigint NOT NULL, source_target_id bigint NOT NULL, final_version bigint NOT NULL, national_target_id bigint NULL, status varchar(20) NOT NULL DEFAULT 'ELIGIBLE', version bigint NOT NULL DEFAULT 0, reason varchar(2000) NOT NULL DEFAULT '',
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
active_national_target_id bigint GENERATED ALWAYS AS (
CASE WHEN status IN ('ELIGIBLE','ATTACHED') THEN national_target_id ELSE NULL END
) STORED,
UNIQUE KEY uk_qualification(source_phase_id,source_target_id,final_version),
UNIQUE KEY uk_national_target(active_national_target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_award_rules (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
rule_id bigint NOT NULL, phase_id bigint NOT NULL, target_type varchar(20) NOT NULL, first_category_id bigint NOT NULL, second_category_id bigint NULL, group_name varchar(255) NULL, award_name varchar(255) NOT NULL, min_score decimal(8,2) NOT NULL, max_score decimal(8,2) NOT NULL, enabled boolean NOT NULL, revision bigint NOT NULL, is_current boolean NOT NULL, created_by bigint NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_award_rule_revision(rule_id,revision), KEY idx_rule_phase(phase_id,is_current,enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_award_batches (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
phase_id bigint NOT NULL, status varchar(20) NOT NULL, input_version bigint NOT NULL, preview_id varchar(36) NOT NULL, supersedes_batch_id bigint NULL, reason varchar(2000) NULL, version bigint NOT NULL, created_by bigint NOT NULL, candidate_json longtext NOT NULL, snapshot_json longtext NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_batch_preview(preview_id), KEY idx_batch_phase(phase_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_awards (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
batch_id bigint NOT NULL, target_id bigint NOT NULL, rule_id bigint NOT NULL, rule_revision bigint NOT NULL, score_version bigint NOT NULL, award_name varchar(255) NOT NULL, final_score decimal(8,2) NOT NULL, target_type varchar(20) NOT NULL, status varchar(20) NOT NULL, snapshot_json longtext NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_batch_target(batch_id,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_award_grants (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
batch_id bigint NOT NULL, award_id bigint NOT NULL, target_id bigint NOT NULL, recipient_type varchar(20) NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_grant_award(award_id), KEY idx_grant_batch(batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_previews (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
token varchar(36) NOT NULL, purpose varchar(80) NOT NULL, operator_id bigint NOT NULL, phase_id bigint NOT NULL, payload_hash varchar(64) NOT NULL, input_version bigint NOT NULL, snapshot longtext NOT NULL, expires_at datetime NOT NULL, consumed_at datetime NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_preview_token(token), KEY idx_preview_expiry(expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cqt_review_command_receipts (
id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
operator_id bigint NOT NULL, operation varchar(100) NOT NULL, request_key varchar(100) NOT NULL, payload_hash varchar(64) NOT NULL, result_json longtext NOT NULL,
created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
UNIQUE KEY uk_receipt(operator_id,operation,request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE cqt_entries ADD COLUMN review_version bigint NOT NULL DEFAULT 0;
ALTER TABLE cqt_evaluation_targets ADD COLUMN review_version bigint NOT NULL DEFAULT 0;

INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1055','0','赛事评审','directory',NULL,NULL,NULL,'ClipboardList','1','1055');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1050','1055','作品初审','menu','/cqt/review/entries','cqt/review/EntryReviewsPage','cqt:entry-review:list','FileText','1','1050');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1060','1055','评审配置','menu','/cqt/review/phases','cqt/review/ReviewPhasesPage','cqt:review:list','Settings','1','1060');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1070','1055','评委管理','menu','/cqt/review/reviewers','cqt/review/ReviewersPage','cqt:reviewer:list','UsersRound','1','1070');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1080','1055','评委分配','menu','/cqt/review/assignments','cqt/review/AssignmentsPage','cqt:assignment:list','FileText','1','1080');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1090','1055','我的评审','menu','/cqt/review/my-tasks','cqt/review/MyReviewTasksPage','cqt:review-task:list','ClipboardList','1','1090');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1100','1055','成绩与晋级','menu','/cqt/review/scores','cqt/review/ScoresPage','cqt:score:list','BarChart3','1','1100');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1110','1055','奖项规则','menu','/cqt/review/award-rules','cqt/review/AwardRulesPage','cqt:award-rule:list','FileText','1','1110');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1120','1055','评奖批次','menu','/cqt/review/award-batches','cqt/review/AwardBatchesPage','cqt:award-batch:list','FileText','1','1120');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1086','1055','分配详情','menu','/cqt/review/assignments/create','cqt/review/AssignmentCreatePage','cqt:assignment:list','FileText','0','1086');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1094','1055','评分详情','menu','/cqt/review/my-tasks/:assignmentId','cqt/review/ReviewTaskPage','cqt:review-task:list','ClipboardList','0','1094');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1126','1055','评奖预览','menu','/cqt/review/award-batches/preview','cqt/review/AwardPreviewPage','cqt:award-batch:list','FileText','0','1126');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1051','1050','决定','button',NULL,NULL,'cqt:entry-review:decide',NULL,'1','1051');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1052','1050','准备对象','button',NULL,NULL,'cqt:entry-review:prepare',NULL,'1','1052');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1061','1060','配置','button',NULL,NULL,'cqt:review:update',NULL,'1','1061');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1062','1060','维度','button',NULL,NULL,'cqt:review:dimension',NULL,'1','1062');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1063','1060','开放关闭','button',NULL,NULL,'cqt:review:open',NULL,'1','1063');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1071','1070','新增范围','button',NULL,NULL,'cqt:reviewer:create',NULL,'1','1071');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1072','1070','更新范围','button',NULL,NULL,'cqt:reviewer:update',NULL,'1','1072');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1081','1086','手工分配','button',NULL,NULL,'cqt:assignment:create',NULL,'1','1081');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1082','1086','自动分配','button',NULL,NULL,'cqt:assignment:auto',NULL,'1','1082');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1083','1080','取消','button',NULL,NULL,'cqt:assignment:cancel',NULL,'1','1083');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1084','1080','恢复','button',NULL,NULL,'cqt:assignment:restore',NULL,'1','1084');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1091','1094','评分','button',NULL,NULL,'cqt:review-task:score',NULL,'1','1091');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1101','1100','汇总','button',NULL,NULL,'cqt:score:calculate',NULL,'1','1101');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1102','1100','更正','button',NULL,NULL,'cqt:score:correct',NULL,'1','1102');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1103','1100','历史','button',NULL,NULL,'cqt:score:history',NULL,'1','1103');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1104','1100','决定晋级','button',NULL,NULL,'cqt:score:advance',NULL,'1','1104');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1105','1100','推进阶段','button',NULL,NULL,'cqt:score:promote',NULL,'1','1105');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1111','1110','新增规则','button',NULL,NULL,'cqt:award-rule:create',NULL,'1','1111');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1112','1110','更新规则','button',NULL,NULL,'cqt:award-rule:update',NULL,'1','1112');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1121','1126','预览','button',NULL,NULL,'cqt:award-batch:preview',NULL,'1','1121');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1122','1126','确认','button',NULL,NULL,'cqt:award-batch:confirm',NULL,'1','1122');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1123','1120','详情','button',NULL,NULL,'cqt:award-batch:detail',NULL,'1','1123');
INSERT INTO sys_menu (id,parent_id,title,type,path,component,permission,icon,visible,sort) VALUES ('1124','1120','撤销','button',NULL,NULL,'cqt:award-batch:revoke',NULL,'1','1124');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 1,id FROM sys_menu WHERE id BETWEEN 1050 AND 1126;
