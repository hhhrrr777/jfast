-- 测试库(H2,MySQL 兼容模式)认证域建表脚本,供 spring.sql.init 使用。
-- 与 db/schema-mysql.sql 等价,去除 MySQL 专有子句(ENGINE/ON UPDATE 等)。

CREATE TABLE IF NOT EXISTS sys_user (
    user_id     BIGINT       NOT NULL AUTO_INCREMENT,
    user_name   VARCHAR(30)  NOT NULL,
    nick_name   VARCHAR(30)  NOT NULL DEFAULT '',
    password    VARCHAR(100) NOT NULL DEFAULT '',
    status      CHAR(1)      NOT NULL DEFAULT '0',
    del_flag    CHAR(1)      NOT NULL DEFAULT '0',
    login_ip    VARCHAR(128) NOT NULL DEFAULT '',
    login_date  DATETIME     NULL,
    create_by   VARCHAR(64)  NOT NULL DEFAULT '',
    create_time DATETIME     NULL,
    update_by   VARCHAR(64)  NOT NULL DEFAULT '',
    update_time DATETIME     NULL,
    remark      VARCHAR(500) NULL,
    PRIMARY KEY (user_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_name ON sys_user (user_name);

CREATE TABLE IF NOT EXISTS sys_refresh_token (
    token_id    BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    device_id   VARCHAR(64) NOT NULL DEFAULT '',
    token       VARCHAR(512) NOT NULL,
    expire_time DATETIME    NOT NULL,
    revoked     CHAR(1)     NOT NULL DEFAULT '0',
    create_time DATETIME    NULL,
    PRIMARY KEY (token_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_token ON sys_refresh_token (token);
CREATE INDEX IF NOT EXISTS idx_refresh_user_device ON sys_refresh_token (user_id, device_id);
