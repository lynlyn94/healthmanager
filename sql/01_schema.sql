-- ============================================================
-- 康复科管理系统 — 数据库建表脚本
-- 版本: v1.0 | 数据库: MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS rehab_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE rehab_db;

-- ============================================================
-- 1. 治疗小组表
-- ============================================================
DROP TABLE IF EXISTS therapy_group;
CREATE TABLE therapy_group (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    group_name  VARCHAR(64)  NOT NULL COMMENT '小组名称',
    leader_id   BIGINT       DEFAULT NULL COMMENT '组长ID(关联user)',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除: 0=正常, 1=删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗小组';

-- ============================================================
-- 2. 用户表
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(32)  NOT NULL COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码(BCrypt)',
    real_name   VARCHAR(32)  NOT NULL COMMENT '真实姓名',
    role        VARCHAR(16)  NOT NULL COMMENT '角色: THERAPIST/NURSE/DOCTOR/ADMIN',
    group_id    BIGINT       DEFAULT NULL COMMENT '所属小组ID',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '电话',
    email       VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 3. 患者表
-- ============================================================
DROP TABLE IF EXISTS patient;
CREATE TABLE patient (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name                  VARCHAR(32)  NOT NULL COMMENT '姓名',
    gender                TINYINT      NOT NULL COMMENT '性别: 0=女, 1=男',
    age                   INT          DEFAULT NULL COMMENT '年龄',
    inpatient_no          VARCHAR(32)  DEFAULT NULL COMMENT '住院号',
    bed_no                VARCHAR(16)  DEFAULT NULL COMMENT '床号',
    admission_date        DATE         DEFAULT NULL COMMENT '入院日期',
    diagnosis             TEXT         DEFAULT NULL COMMENT '诊断信息',
    allergy_history       TEXT         DEFAULT NULL COMMENT '过敏史',
    contact_phone         VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    emergency_contact     VARCHAR(32)  DEFAULT NULL COMMENT '紧急联系人',
    emergency_phone       VARCHAR(20)  DEFAULT NULL COMMENT '紧急联系人电话',
    attending_therapist_id BIGINT      DEFAULT NULL COMMENT '负责治疗师ID',
    attending_doctor_id   BIGINT       DEFAULT NULL COMMENT '负责医生ID',
    status                VARCHAR(16)  NOT NULL DEFAULT 'IN_HOSPITAL' COMMENT '状态: IN_HOSPITAL/DISCHARGED',
    discharge_date        DATE         DEFAULT NULL COMMENT '出院日期',
    remark                TEXT         DEFAULT NULL COMMENT '备注',
    is_deleted            TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_therapist (attending_therapist_id),
    INDEX idx_inpatient_no (inpatient_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者表';

-- ============================================================
-- 4. 评估量表模板表
-- ============================================================
DROP TABLE IF EXISTS assessment_template;
CREATE TABLE assessment_template (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    abbreviation  VARCHAR(16)  DEFAULT NULL COMMENT '缩写(FMA/BI/MAS/BBS/VAS)',
    category      VARCHAR(32)  DEFAULT NULL COMMENT '分类',
    items         JSON         DEFAULT NULL COMMENT '评分项定义(JSON数组)',
    scoring_rule  VARCHAR(255) DEFAULT NULL COMMENT '计分规则说明',
    max_score     DECIMAL(8,2) DEFAULT NULL COMMENT '满分',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估量表模板';

-- ============================================================
-- 5. 量表评估表
-- ============================================================
DROP TABLE IF EXISTS assessment;
CREATE TABLE assessment (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    patient_id    BIGINT       NOT NULL COMMENT '患者ID',
    template_id   BIGINT       NOT NULL COMMENT '模板ID',
    assessor_id   BIGINT       NOT NULL COMMENT '评估人ID',
    assess_date   DATE         NOT NULL COMMENT '评估日期',
    total_score   DECIMAL(8,2) DEFAULT NULL COMMENT '总分',
    detail        JSON         DEFAULT NULL COMMENT '各评分项得分(JSON)',
    conclusion    TEXT         DEFAULT NULL COMMENT '评估结论',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_patient (patient_id),
    INDEX idx_template (template_id),
    INDEX idx_assessor (assessor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='量表评估';

-- ============================================================
-- 6. 治疗目标表
-- ============================================================
DROP TABLE IF EXISTS treatment_goal;
CREATE TABLE treatment_goal (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    patient_id  BIGINT       NOT NULL COMMENT '患者ID',
    goal_type   VARCHAR(16)  NOT NULL COMMENT '类型: SHORT_TERM/LONG_TERM',
    content     TEXT         NOT NULL COMMENT '目标内容',
    target_date DATE         DEFAULT NULL COMMENT '目标达成日期',
    status      VARCHAR(16)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/ACHIEVED/ABANDONED',
    creator_id  BIGINT       NOT NULL COMMENT '创建人ID',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_patient (patient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗目标';

-- ============================================================
-- 7. 治疗方案表
-- ============================================================
DROP TABLE IF EXISTS treatment_plan;
CREATE TABLE treatment_plan (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    patient_id      BIGINT       NOT NULL COMMENT '患者ID',
    plan_name       VARCHAR(128) NOT NULL COMMENT '方案名称',
    treatment_items JSON         DEFAULT NULL COMMENT '治疗项目列表(JSON)',
    frequency       VARCHAR(64)  DEFAULT NULL COMMENT '治疗频次(每日/每周)',
    daily_count     INT          DEFAULT 1 COMMENT '每日次数',
    period_start    DATE         DEFAULT NULL COMMENT '开始日期',
    period_end      DATE         DEFAULT NULL COMMENT '结束日期',
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/SUBMITTED/REVIEWED',
    creator_id      BIGINT       NOT NULL COMMENT '制定人ID',
    reviewer_id     BIGINT       DEFAULT NULL COMMENT '审阅人ID(医生)',
    review_comment  TEXT         DEFAULT NULL COMMENT '审阅意见',
    submit_time     DATETIME     DEFAULT NULL COMMENT '提交时间',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_patient (patient_id),
    INDEX idx_status (status),
    INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗方案';

-- ============================================================
-- 8. 医嘱表
-- ============================================================
DROP TABLE IF EXISTS medical_order;
CREATE TABLE medical_order (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    patient_id      BIGINT       NOT NULL COMMENT '患者ID',
    doctor_id       BIGINT       NOT NULL COMMENT '医生ID',
    plan_id         BIGINT       DEFAULT NULL COMMENT '关联治疗方案ID',
    order_type      VARCHAR(32)  NOT NULL COMMENT '医嘱类型',
    treatment_item  VARCHAR(128) NOT NULL COMMENT '治疗项目',
    frequency       VARCHAR(64)  NOT NULL COMMENT '频次',
    daily_count     INT          DEFAULT 1 COMMENT '每日次数',
    period_start    DATE         NOT NULL COMMENT '开始日期',
    period_end      DATE         NOT NULL COMMENT '结束日期',
    note            TEXT         DEFAULT NULL COMMENT '备注',
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PENDING_REVIEW/APPROVED/REJECTED/CANCELLED',
    review_comment  TEXT         DEFAULT NULL COMMENT '审阅意见/退回原因',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_patient (patient_id),
    INDEX idx_doctor (doctor_id),
    INDEX idx_status (status),
    INDEX idx_patient_status (patient_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医嘱';

-- ============================================================
-- 9. 治疗任务表
-- ============================================================
DROP TABLE IF EXISTS task;
CREATE TABLE task (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    patient_id        BIGINT       NOT NULL COMMENT '患者ID',
    order_id          BIGINT       DEFAULT NULL COMMENT '关联医嘱ID',
    therapist_id      BIGINT       NOT NULL COMMENT '执行治疗师ID',
    group_id          BIGINT       DEFAULT NULL COMMENT '治疗小组ID',
    task_date         DATE         NOT NULL COMMENT '任务日期',
    time_slot         VARCHAR(32)  DEFAULT NULL COMMENT '时间段(上午/下午/具体时间)',
    treatment_item    VARCHAR(128) NOT NULL COMMENT '治疗项目',
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/IN_PROGRESS/VERIFIED/REVOKED',
    start_time        DATETIME     DEFAULT NULL COMMENT '开始执行时间',
    verification_time DATETIME     DEFAULT NULL COMMENT '核销时间',
    revoke_time       DATETIME     DEFAULT NULL COMMENT '撤销时间',
    revoke_reason     VARCHAR(255) DEFAULT NULL COMMENT '撤销原因',
    note              TEXT         DEFAULT NULL COMMENT '备注',
    version           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    is_deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_therapist_date (therapist_id, task_date),
    INDEX idx_patient_date (patient_id, task_date),
    INDEX idx_group_date (group_id, task_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗任务';

-- ============================================================
-- 10. 核销记录表
-- ============================================================
DROP TABLE IF EXISTS task_verification;
CREATE TABLE task_verification (
    id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id      BIGINT   NOT NULL COMMENT '任务ID',
    verifier_id  BIGINT   NOT NULL COMMENT '核销人ID',
    verify_time  DATETIME NOT NULL COMMENT '核销时间',
    revoked      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否已撤销: 0=否, 1=是',
    revoke_time  DATETIME DEFAULT NULL COMMENT '撤销时间',
    revoke_reason VARCHAR(255) DEFAULT NULL COMMENT '撤销原因',
    is_deleted   TINYINT  NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_task (task_id),
    INDEX idx_verifier (verifier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='核销记录';

-- ============================================================
-- 11. 工作量统计表
-- ============================================================
DROP TABLE IF EXISTS workload_stat;
CREATE TABLE workload_stat (
    id               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id          BIGINT   NOT NULL COMMENT '用户ID',
    stat_date        DATE     NOT NULL COMMENT '统计日期',
    treatment_count  INT      NOT NULL DEFAULT 0 COMMENT '完成治疗项目数',
    patient_count    INT      NOT NULL DEFAULT 0 COMMENT '服务患者数',
    treatment_type   VARCHAR(32) DEFAULT NULL COMMENT '治疗类型',
    is_deleted       TINYINT  NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_user_date (user_id, stat_date),
    INDEX idx_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作量统计';

-- ============================================================
-- 12. 治疗记录表
-- ============================================================
DROP TABLE IF EXISTS treatment_record;
CREATE TABLE treatment_record (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    patient_id     BIGINT       NOT NULL COMMENT '患者ID',
    task_id        BIGINT       DEFAULT NULL COMMENT '关联任务ID',
    therapist_id   BIGINT       NOT NULL COMMENT '治疗师ID',
    treatment_date DATE         NOT NULL COMMENT '治疗日期',
    treatment_item VARCHAR(128) NOT NULL COMMENT '治疗项目',
    duration       INT          DEFAULT NULL COMMENT '治疗时长(分钟)',
    note           TEXT         DEFAULT NULL COMMENT '治疗记录',
    is_deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_patient (patient_id),
    INDEX idx_therapist (therapist_id),
    INDEX idx_date (treatment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='治疗记录';

-- ============================================================
-- 13. 患者日程表 (聚合视图物理化)
-- ============================================================
DROP TABLE IF EXISTS patient_schedule;
CREATE TABLE patient_schedule (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    patient_id     BIGINT       NOT NULL COMMENT '患者ID',
    schedule_date  DATE         NOT NULL COMMENT '日程日期',
    time_slot      VARCHAR(32)  DEFAULT NULL COMMENT '时间段',
    event_type     VARCHAR(32)  NOT NULL COMMENT '事件类型: ORDER/TASK/ASSESSMENT/OTHER',
    source_id      BIGINT       DEFAULT NULL COMMENT '关联来源ID(医嘱ID/任务ID/评估ID)',
    therapist_id   BIGINT       DEFAULT NULL COMMENT '负责治疗师ID',
    title          VARCHAR(255) NOT NULL COMMENT '标题',
    description    TEXT         DEFAULT NULL COMMENT '描述',
    status         VARCHAR(16)  DEFAULT 'PENDING' COMMENT '状态',
    is_deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_patient_date (patient_id, schedule_date),
    INDEX idx_therapist_date (therapist_id, schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者日程';

-- ============================================================
-- 14. 操作日志表
-- ============================================================
DROP TABLE IF EXISTS operation_log;
CREATE TABLE operation_log (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT        DEFAULT NULL COMMENT '操作用户ID',
    username      VARCHAR(32)   DEFAULT NULL COMMENT '操作用户名',
    module        VARCHAR(64)   DEFAULT NULL COMMENT '操作模块',
    action        VARCHAR(64)   DEFAULT NULL COMMENT '操作类型(CREATE/UPDATE/DELETE/VERIFY/REVOKE等)',
    target_type   VARCHAR(64)   DEFAULT NULL COMMENT '目标类型',
    target_id     BIGINT        DEFAULT NULL COMMENT '目标ID',
    description   TEXT          DEFAULT NULL COMMENT '操作描述',
    request_ip    VARCHAR(64)   DEFAULT NULL COMMENT '请求IP',
    request_url   VARCHAR(255)  DEFAULT NULL COMMENT '请求URL',
    request_param TEXT          DEFAULT NULL COMMENT '请求参数(JSON)',
    cost_time     BIGINT        DEFAULT NULL COMMENT '耗时(ms)',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_user (user_id),
    INDEX idx_time (create_time),
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志';

-- ============================================================
-- 15. 系统字典表
-- ============================================================
DROP TABLE IF EXISTS system_dict;
CREATE TABLE system_dict (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_type   VARCHAR(64)  NOT NULL COMMENT '字典类型',
    dict_code   VARCHAR(64)  NOT NULL COMMENT '字典编码',
    dict_value  VARCHAR(128) NOT NULL COMMENT '字典值',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=停用, 1=启用',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_code (dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统字典';
