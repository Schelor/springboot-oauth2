-- SQL Server DDL: all tables managed here (not by Hibernate ddl-auto).
-- Runs on every startup via spring.sql.init.mode=always.
-- Separator is GO (configured in application.yaml), not semicolon.
-- IF OBJECT_ID guard makes every block idempotent.

-- ============================================================
-- 1. OAuth2 客户端注册表（对应 SysOAuthClientPO @Entity）
-- ============================================================
IF OBJECT_ID('dbo.sys_oauth_client', 'U') IS NULL
BEGIN
    CREATE TABLE sys_oauth_client (
        id              BIGINT          NOT NULL IDENTITY(1,1),
        client_id       NVARCHAR(100)   NOT NULL,
        client_secret   NVARCHAR(200)   NOT NULL,
        scopes          NVARCHAR(500)   DEFAULT NULL,
        description     NVARCHAR(255)   DEFAULT NULL,

        CONSTRAINT pk_sys_oauth_client        PRIMARY KEY (id),
        CONSTRAINT uq_sys_oauth_client_id     UNIQUE (client_id)
    )
END
GO

-- ============================================================
-- 2. OAuth2 授权/Token 记录表（由 JdbcOAuth2AuthorizationService 管理）
-- ============================================================
IF OBJECT_ID('dbo.oauth2_authorization', 'U') IS NULL
BEGIN
    CREATE TABLE oauth2_authorization (
        id                              NVARCHAR(100)  NOT NULL,
        registered_client_id            NVARCHAR(100)  NOT NULL,
        principal_name                  NVARCHAR(200)  NOT NULL,
        authorization_grant_type        NVARCHAR(100)  NOT NULL,
        authorized_scopes               NVARCHAR(1000) DEFAULT NULL,
        attributes                      NVARCHAR(MAX)  DEFAULT NULL,
        state                           NVARCHAR(500)  DEFAULT NULL,

        -- Access Token
        access_token_value              NVARCHAR(MAX)  DEFAULT NULL,
        access_token_issued_at          DATETIME2(6)   DEFAULT NULL,
        access_token_expires_at         DATETIME2(6)   DEFAULT NULL,
        access_token_metadata           NVARCHAR(MAX)  DEFAULT NULL,
        access_token_type               NVARCHAR(100)  DEFAULT NULL,
        access_token_scopes             NVARCHAR(1000) DEFAULT NULL,

        -- Refresh Token（client_credentials 通常不颁发，预留）
        refresh_token_value             NVARCHAR(MAX)  DEFAULT NULL,
        refresh_token_issued_at         DATETIME2(6)   DEFAULT NULL,
        refresh_token_expires_at        DATETIME2(6)   DEFAULT NULL,
        refresh_token_metadata          NVARCHAR(MAX)  DEFAULT NULL,

        -- Authorization Code（authorization_code 流程用）
        authorization_code_value        NVARCHAR(MAX)  DEFAULT NULL,
        authorization_code_issued_at    DATETIME2(6)   DEFAULT NULL,
        authorization_code_expires_at   DATETIME2(6)   DEFAULT NULL,
        authorization_code_metadata     NVARCHAR(MAX)  DEFAULT NULL,

        -- OIDC ID Token
        oidc_id_token_value             NVARCHAR(MAX)  DEFAULT NULL,
        oidc_id_token_issued_at         DATETIME2(6)   DEFAULT NULL,
        oidc_id_token_expires_at        DATETIME2(6)   DEFAULT NULL,
        oidc_id_token_metadata          NVARCHAR(MAX)  DEFAULT NULL,

        -- Device Code
        user_code_value                 NVARCHAR(MAX)  DEFAULT NULL,
        user_code_issued_at             DATETIME2(6)   DEFAULT NULL,
        user_code_expires_at            DATETIME2(6)   DEFAULT NULL,
        user_code_metadata              NVARCHAR(MAX)  DEFAULT NULL,
        device_code_value               NVARCHAR(MAX)  DEFAULT NULL,
        device_code_issued_at           DATETIME2(6)   DEFAULT NULL,
        device_code_expires_at          DATETIME2(6)   DEFAULT NULL,
        device_code_metadata            NVARCHAR(MAX)  DEFAULT NULL,

        CONSTRAINT pk_oauth2_authorization PRIMARY KEY (id)
    )
END
GO

-- ============================================================
-- 3. oauth2_authorization 索引（幂等：索引不存在才创建）
-- ============================================================
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'idx_oauth2_auth_principal'
                 AND object_id = OBJECT_ID('dbo.oauth2_authorization'))
BEGIN
    CREATE INDEX idx_oauth2_auth_principal
        ON oauth2_authorization (principal_name)
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'idx_oauth2_auth_expires'
                 AND object_id = OBJECT_ID('dbo.oauth2_authorization'))
BEGIN
    CREATE INDEX idx_oauth2_auth_expires
        ON oauth2_authorization (access_token_expires_at)
END
GO