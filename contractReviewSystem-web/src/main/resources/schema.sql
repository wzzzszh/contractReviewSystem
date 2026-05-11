CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password VARCHAR(255) DEFAULT NULL COMMENT '密码',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(255) DEFAULT NULL COMMENT '密码';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色主键',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0停用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限主键',
    permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

INSERT IGNORE INTO sys_role (role_code, role_name, status) VALUES
('ADMIN', '管理员', 1),
('USER', '普通用户', 1);

INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES
('user:create', '创建用户'),
('user:view', '查看用户详情'),
('user:list', '查看用户列表'),
('file:upload', '上传文件'),
('file:download', '下载文件'),
('file:list', '查看文件记录'),
('file:delete', '删除文件'),
('file:record:create', '创建文件记录'),
('review:modify', '合同审查修改'),
('monitor:view', '查看系统监控');

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p
WHERE r.role_code = 'ADMIN';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'file:upload',
    'file:download',
    'file:list',
    'file:delete',
    'file:record:create',
    'review:modify'
)
WHERE r.role_code = 'USER';

INSERT IGNORE INTO sys_user (username, password, nickname, status)
VALUES ('admin', '$2a$10$fDzgj6WCx7huzKdARe56D.mXDjU0witDxsxwfTHqeQl.3k4Lq38Tq', '管理员', 1);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = 'USER'
WHERE u.username <> 'admin';

CREATE TABLE IF NOT EXISTS file_storage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文件记录主键',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(1024) NOT NULL COMMENT '持久化文件地址',
    file_category VARCHAR(32) NOT NULL COMMENT '文件分类: uploaded/modified',
    file_status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '文件状态: active/expired/deleted/temp',
    source_file_id BIGINT DEFAULT NULL COMMENT '原始文件ID, 修改后文件可关联原文件',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小, 单位字节',
    content_type VARCHAR(128) DEFAULT NULL COMMENT '文件 MIME 类型',
    expire_time DATETIME DEFAULT NULL COMMENT '临时文件过期时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记: 0未删除 1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_file_storage_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_file_storage_source FOREIGN KEY (source_file_id) REFERENCES file_storage (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件存储地址表';
CREATE TABLE IF NOT EXISTS review_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'review task id',
    user_id BIGINT NOT NULL COMMENT 'owner user id',
    source_file_id BIGINT NOT NULL COMMENT 'source file id',
    result_file_id BIGINT DEFAULT NULL COMMENT 'result file id',
    task_type VARCHAR(32) NOT NULL COMMENT 'task type',
    status VARCHAR(32) NOT NULL COMMENT 'pending/running/success/failed',
    progress TINYINT NOT NULL DEFAULT 0 COMMENT 'progress 0-100',
    perspective VARCHAR(32) DEFAULT NULL COMMENT 'review perspective',
    user_focus TEXT DEFAULT NULL COMMENT 'user focus',
    risk_report LONGTEXT DEFAULT NULL COMMENT 'risk report',
    generated_requirement LONGTEXT DEFAULT NULL COMMENT 'generated requirement',
    applied_operation_count INT DEFAULT NULL COMMENT 'applied patch operation count',
    skipped_operation_count INT DEFAULT NULL COMMENT 'skipped patch operation count',
    skipped_operation_messages LONGTEXT DEFAULT NULL COMMENT 'skipped patch operation messages json',
    retryable TINYINT NOT NULL DEFAULT 0 COMMENT 'whether task can be retried manually',
    retry_count TINYINT NOT NULL DEFAULT 0 COMMENT 'manual retry count',
    max_retry TINYINT NOT NULL DEFAULT 3 COMMENT 'max manual retry count',
    last_error_code VARCHAR(64) DEFAULT NULL COMMENT 'last error code',
    error_message TEXT DEFAULT NULL COMMENT 'error message',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    KEY idx_review_task_user_time (user_id, create_time),
    KEY idx_review_task_status (status),
    KEY idx_review_task_source_file (source_file_id),
    CONSTRAINT fk_review_task_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_review_task_source_file FOREIGN KEY (source_file_id) REFERENCES file_storage (id),
    CONSTRAINT fk_review_task_result_file FOREIGN KEY (result_file_id) REFERENCES file_storage (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='review task table';
