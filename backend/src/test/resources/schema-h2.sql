CREATE TABLE IF NOT EXISTS therapy_group (
    id          BIGINT AUTO_INCREMENT,
    group_name  VARCHAR(64)  NOT NULL,
    leader_id   BIGINT       DEFAULT NULL,
    description VARCHAR(255) DEFAULT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `user` (
    id          BIGINT AUTO_INCREMENT,
    username    VARCHAR(32)  NOT NULL,
    password    VARCHAR(128) NOT NULL,
    real_name   VARCHAR(32)  NOT NULL,
    role        VARCHAR(16)  NOT NULL,
    group_id    BIGINT       DEFAULT NULL,
    phone       VARCHAR(20)  DEFAULT NULL,
    email       VARCHAR(64)  DEFAULT NULL,
    avatar      VARCHAR(255) DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
);

CREATE TABLE IF NOT EXISTS patient (
    id                    BIGINT AUTO_INCREMENT,
    name                  VARCHAR(32)  NOT NULL,
    gender                TINYINT      NOT NULL,
    age                   INT          DEFAULT NULL,
    inpatient_no          VARCHAR(32)  DEFAULT NULL,
    bed_no                VARCHAR(16)  DEFAULT NULL,
    admission_date        DATE         DEFAULT NULL,
    diagnosis             TEXT         DEFAULT NULL,
    allergy_history       TEXT         DEFAULT NULL,
    contact_phone         VARCHAR(20)  DEFAULT NULL,
    emergency_contact     VARCHAR(32)  DEFAULT NULL,
    emergency_phone       VARCHAR(20)  DEFAULT NULL,
    attending_therapist_id BIGINT      DEFAULT NULL,
    attending_doctor_id   BIGINT       DEFAULT NULL,
    status                VARCHAR(16)  NOT NULL DEFAULT 'IN_HOSPITAL',
    discharge_date        DATE         DEFAULT NULL,
    remark                TEXT         DEFAULT NULL,
    is_deleted            TINYINT      NOT NULL DEFAULT 0,
    create_time           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS assessment_template (
    id            BIGINT AUTO_INCREMENT,
    template_name VARCHAR(128) NOT NULL,
    abbreviation  VARCHAR(16)  DEFAULT NULL,
    category      VARCHAR(32)  DEFAULT NULL,
    items         TEXT         DEFAULT NULL,
    scoring_rule  VARCHAR(255) DEFAULT NULL,
    max_score     DECIMAL(8,2) DEFAULT NULL,
    status        TINYINT      NOT NULL DEFAULT 1,
    is_deleted    TINYINT      NOT NULL DEFAULT 0,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS assessment (
    id            BIGINT AUTO_INCREMENT,
    patient_id    BIGINT       NOT NULL,
    template_id   BIGINT       NOT NULL,
    assessor_id   BIGINT       NOT NULL,
    assess_date   DATE         NOT NULL,
    total_score   DECIMAL(8,2) DEFAULT NULL,
    detail        TEXT         DEFAULT NULL,
    conclusion    TEXT         DEFAULT NULL,
    is_deleted    TINYINT      NOT NULL DEFAULT 0,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS treatment_goal (
    id          BIGINT AUTO_INCREMENT,
    patient_id  BIGINT       NOT NULL,
    goal_type   VARCHAR(16)  NOT NULL,
    content     TEXT         NOT NULL,
    target_date DATE         DEFAULT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'IN_PROGRESS',
    creator_id  BIGINT       NOT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS treatment_plan (
    id              BIGINT AUTO_INCREMENT,
    patient_id      BIGINT       NOT NULL,
    plan_name       VARCHAR(128) NOT NULL,
    treatment_items TEXT         DEFAULT NULL,
    frequency       VARCHAR(64)  DEFAULT NULL,
    daily_count     INT          DEFAULT 1,
    period_start    DATE         DEFAULT NULL,
    period_end      DATE         DEFAULT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    creator_id      BIGINT       NOT NULL,
    reviewer_id     BIGINT       DEFAULT NULL,
    review_comment  TEXT         DEFAULT NULL,
    submit_time     TIMESTAMP    DEFAULT NULL,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS medical_order (
    id              BIGINT AUTO_INCREMENT,
    patient_id      BIGINT       NOT NULL,
    doctor_id       BIGINT       NOT NULL,
    plan_id         BIGINT       DEFAULT NULL,
    order_type      VARCHAR(32)  NOT NULL,
    treatment_item  VARCHAR(128) NOT NULL,
    frequency       VARCHAR(64)  NOT NULL,
    daily_count     INT          DEFAULT 1,
    period_start    DATE         NOT NULL,
    period_end      DATE         NOT NULL,
    note            TEXT         DEFAULT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    review_comment  TEXT         DEFAULT NULL,
    is_deleted      TINYINT      NOT NULL DEFAULT 0,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS task (
    id                BIGINT AUTO_INCREMENT,
    patient_id        BIGINT       NOT NULL,
    order_id          BIGINT       DEFAULT NULL,
    therapist_id      BIGINT       NOT NULL,
    group_id          BIGINT       DEFAULT NULL,
    task_date         DATE         NOT NULL,
    time_slot         VARCHAR(32)  DEFAULT NULL,
    treatment_item    VARCHAR(128) NOT NULL,
    status            VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    start_time        TIMESTAMP    DEFAULT NULL,
    verification_time TIMESTAMP    DEFAULT NULL,
    revoke_time       TIMESTAMP    DEFAULT NULL,
    revoke_reason     VARCHAR(255) DEFAULT NULL,
    note              TEXT         DEFAULT NULL,
    version           INT          NOT NULL DEFAULT 0,
    is_deleted        TINYINT      NOT NULL DEFAULT 0,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS task_verification (
    id           BIGINT AUTO_INCREMENT,
    task_id      BIGINT   NOT NULL,
    verifier_id  BIGINT   NOT NULL,
    verify_time  TIMESTAMP NOT NULL,
    revoked      TINYINT  NOT NULL DEFAULT 0,
    revoke_time  TIMESTAMP DEFAULT NULL,
    revoke_reason VARCHAR(255) DEFAULT NULL,
    is_deleted   TINYINT  NOT NULL DEFAULT 0,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS workload_stat (
    id               BIGINT AUTO_INCREMENT,
    user_id          BIGINT   NOT NULL,
    stat_date        DATE     NOT NULL,
    treatment_count  INT      NOT NULL DEFAULT 0,
    patient_count    INT      NOT NULL DEFAULT 0,
    treatment_type   VARCHAR(32) DEFAULT NULL,
    is_deleted       TINYINT  NOT NULL DEFAULT 0,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS treatment_record (
    id             BIGINT AUTO_INCREMENT,
    patient_id     BIGINT       NOT NULL,
    task_id        BIGINT       DEFAULT NULL,
    therapist_id   BIGINT       NOT NULL,
    treatment_date DATE         NOT NULL,
    treatment_item VARCHAR(128) NOT NULL,
    duration       INT          DEFAULT NULL,
    note           TEXT         DEFAULT NULL,
    is_deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS patient_schedule (
    id             BIGINT AUTO_INCREMENT,
    patient_id     BIGINT       NOT NULL,
    schedule_date  DATE         NOT NULL,
    time_slot      VARCHAR(32)  DEFAULT NULL,
    event_type     VARCHAR(32)  NOT NULL,
    source_id      BIGINT       DEFAULT NULL,
    therapist_id   BIGINT       DEFAULT NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT         DEFAULT NULL,
    status         VARCHAR(16)  DEFAULT 'PENDING',
    is_deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS operation_log (
    id            BIGINT AUTO_INCREMENT,
    user_id       BIGINT        DEFAULT NULL,
    username      VARCHAR(32)   DEFAULT NULL,
    module        VARCHAR(64)   DEFAULT NULL,
    action        VARCHAR(64)   DEFAULT NULL,
    target_type   VARCHAR(64)   DEFAULT NULL,
    target_id     BIGINT        DEFAULT NULL,
    description   TEXT          DEFAULT NULL,
    request_ip    VARCHAR(64)   DEFAULT NULL,
    request_url   VARCHAR(255)  DEFAULT NULL,
    request_param TEXT          DEFAULT NULL,
    cost_time     BIGINT        DEFAULT NULL,
    create_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS system_dict (
    id          BIGINT AUTO_INCREMENT,
    dict_type   VARCHAR(64)  NOT NULL,
    dict_code   VARCHAR(64)  NOT NULL,
    dict_value  VARCHAR(128) NOT NULL,
    sort_order  INT          DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(255) DEFAULT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS notification (
    id          BIGINT AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         DEFAULT NULL,
    type        VARCHAR(32)  DEFAULT NULL,
    is_read     TINYINT      NOT NULL DEFAULT 0,
    source_id   BIGINT       DEFAULT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
