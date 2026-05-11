CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'user primary key',
    username VARCHAR(64) NOT NULL COMMENT 'username',
    password VARCHAR(255) DEFAULT NULL COMMENT 'password',
    nickname VARCHAR(64) DEFAULT NULL COMMENT 'nickname',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status: 1 active, 0 disabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user table';

ALTER TABLE sys_user MODIFY COLUMN password VARCHAR(255) DEFAULT NULL COMMENT 'password';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'role primary key',
    role_code VARCHAR(64) NOT NULL COMMENT 'role code',
    role_name VARCHAR(64) NOT NULL COMMENT 'role name',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'status: 1 active, 0 disabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role table';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'permission primary key',
    permission_code VARCHAR(128) NOT NULL COMMENT 'permission code',
    permission_name VARCHAR(128) NOT NULL COMMENT 'permission name',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    UNIQUE KEY uk_sys_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='permission table';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT 'user id',
    role_id BIGINT NOT NULL COMMENT 'role id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user-role mapping';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL COMMENT 'role id',
    permission_id BIGINT NOT NULL COMMENT 'permission id',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='role-permission mapping';

INSERT IGNORE INTO sys_role (role_code, role_name, status) VALUES
('ADMIN', 'Administrator', 1),
('USER', 'Normal User', 1);

INSERT IGNORE INTO sys_permission (permission_code, permission_name) VALUES
('user:create', 'Create user'),
('user:view', 'View user detail'),
('user:list', 'List users'),
('file:upload', 'Upload file'),
('file:download', 'Download file'),
('file:list', 'List files'),
('file:delete', 'Delete file'),
('file:record:create', 'Create file record'),
('review:modify', 'Modify review content'),
('monitor:view', 'View monitor info'),
('llm:select', 'Select llm provider'),
('llm:manage', 'Manage llm provider');

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
    'review:modify',
    'llm:select'
)
WHERE r.role_code = 'USER';

INSERT IGNORE INTO sys_user (username, password, nickname, status)
VALUES ('admin', '$2a$10$fDzgj6WCx7huzKdARe56D.mXDjU0witDxsxwfTHqeQl.3k4Lq38Tq', 'Administrator', 1);

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
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'file record primary key',
    user_id BIGINT NOT NULL COMMENT 'owner user id',
    file_name VARCHAR(255) NOT NULL COMMENT 'file name',
    file_path VARCHAR(1024) NOT NULL COMMENT 'storage path',
    file_category VARCHAR(32) NOT NULL COMMENT 'category: uploaded/modified',
    file_status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'status: active/expired/deleted/temp',
    source_file_id BIGINT DEFAULT NULL COMMENT 'source file id for derived files',
    file_size BIGINT DEFAULT NULL COMMENT 'size in bytes',
    content_type VARCHAR(128) DEFAULT NULL COMMENT 'mime type',
    expire_time DATETIME DEFAULT NULL COMMENT 'expire time for temporary files',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'soft delete flag: 0 no, 1 yes',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    CONSTRAINT fk_file_storage_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_file_storage_source FOREIGN KEY (source_file_id) REFERENCES file_storage (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='file storage table';

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
    llm_provider VARCHAR(32) DEFAULT NULL COMMENT 'llm provider used by this task',
    start_time DATETIME DEFAULT NULL COMMENT 'review execution start time',
    finish_time DATETIME DEFAULT NULL COMMENT 'review execution finish time',
    duration_ms BIGINT DEFAULT NULL COMMENT 'review execution duration in milliseconds',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    KEY idx_review_task_user_time (user_id, create_time),
    KEY idx_review_task_status (status),
    KEY idx_review_task_source_file (source_file_id),
    CONSTRAINT fk_review_task_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_review_task_source_file FOREIGN KEY (source_file_id) REFERENCES file_storage (id),
    CONSTRAINT fk_review_task_result_file FOREIGN KEY (result_file_id) REFERENCES file_storage (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='review task table';
