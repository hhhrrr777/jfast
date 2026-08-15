-- jfast baseline 认证域建表脚本(MySQL 方言)
-- 设计参照 RuoYi-Vue(MIT,见仓库根 NOTICE),按 jfast 工程规范重新设计,未逐字搬运 DDL。
-- 执行方式:启动前手工执行,或配置 spring.sql.init 自动执行。

-- 用户信息表(S2-2 仅承载登录;增删改查归 S2-3 权限域)
CREATE TABLE IF NOT EXISTS sys_user (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    user_name     VARCHAR(30)  NOT NULL                COMMENT '登录账号',
    nick_name     VARCHAR(30)  NOT NULL DEFAULT ''     COMMENT '昵称',
    password      VARCHAR(100) NOT NULL DEFAULT ''     COMMENT 'BCrypt 密码哈希',
    status        CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态(0正常 1停用)',
    del_flag      CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志(0存在 2删除)',
    login_ip      VARCHAR(128) NOT NULL DEFAULT ''     COMMENT '最后登录IP',
    login_date    DATETIME     NULL                    COMMENT '最后登录时间',
    create_by     VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '创建者',
    create_time   DATETIME     NULL                    COMMENT '创建时间',
    update_by     VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '更新者',
    update_time   DATETIME     NULL                    COMMENT '更新时间',
    remark        VARCHAR(500) NULL                    COMMENT '备注',
    PRIMARY KEY (user_id),
    UNIQUE KEY uk_sys_user_name (user_name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户信息表';

-- 登录刷新令牌表(双 token:access 无状态,refresh 落库可吊销)
-- 按(用户, 设备/会话)多行;同设备重登覆盖,异设备共存。
CREATE TABLE IF NOT EXISTS sys_refresh_token (
    token_id    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '令牌ID',
    user_id     BIGINT      NOT NULL                COMMENT '用户ID',
    device_id   VARCHAR(64) NOT NULL DEFAULT ''     COMMENT '设备/会话标识',
    token       VARCHAR(512) NOT NULL               COMMENT 'refresh token(唯一)',
    expire_time DATETIME    NOT NULL                COMMENT '过期时间',
    revoked     CHAR(1)     NOT NULL DEFAULT '0'    COMMENT '是否吊销(0正常 1吊销)',
    create_time DATETIME    NULL                    COMMENT '创建时间',
    PRIMARY KEY (token_id),
    UNIQUE KEY uk_refresh_token (token),
    KEY idx_refresh_user_device (user_id, device_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='登录刷新令牌表';
