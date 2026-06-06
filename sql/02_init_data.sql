-- ============================================================
-- 康复科管理系统 — 初始数据
-- ============================================================

USE rehab_db;

-- ============================================================
-- 治疗小组
-- ============================================================
INSERT INTO therapy_group (id, group_name, description) VALUES
(1, '物理治疗一组', '负责物理治疗(PT)相关康复训练'),
(2, '物理治疗二组', '负责物理治疗(PT)相关康复训练'),
(3, '作业治疗组', '负责作业治疗(OT)相关康复训练'),
(4, '言语治疗组', '负责言语治疗(ST)相关康复训练');

-- ============================================================
-- 用户 (密码均为 123456，BCrypt 加密)
-- 默认密码: rehab123
-- ============================================================
INSERT INTO `user` (id, username, password, real_name, role, group_id, phone, email, status) VALUES
(1,  'admin',       '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '系统管理员', 'ADMIN',     NULL, '13800000001', 'admin@rehab.com',       1),
(2,  'doctor1',     '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '张医生',     'DOCTOR',    NULL, '13800000002', 'doctor1@rehab.com',     1),
(3,  'doctor2',     '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '李医生',     'DOCTOR',    NULL, '13800000003', 'doctor2@rehab.com',     1),
(4,  'therapist1',  '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '王治疗师',   'THERAPIST', 1,    '13800000004', 'therapist1@rehab.com',  1),
(5,  'therapist2',  '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '赵治疗师',   'THERAPIST', 1,    '13800000005', 'therapist2@rehab.com',  1),
(6,  'therapist3',  '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '孙治疗师',   'THERAPIST', 2,    '13800000006', 'therapist3@rehab.com',  1),
(7,  'therapist4',  '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '陈治疗师',   'THERAPIST', 3,    '13800000007', 'therapist4@rehab.com',  1),
(8,  'nurse1',      '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '刘护士',     'NURSE',     1,    '13800000008', 'nurse1@rehab.com',      1),
(9,  'nurse2',      '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '周护士',     'NURSE',     3,    '13800000009', 'nurse2@rehab.com',      1),
(10, 'therapist5',  '$2b$10$LbzXbOtpCBn5Xn0ruoqM6umwQMeQ6mhZKYPisrmLC8.fKvnjqPQr2', '吴治疗师',   'THERAPIST', 4,    '13800000010', 'therapist5@rehab.com',  1);

-- ============================================================
-- 更新组长关联
-- ============================================================
UPDATE therapy_group SET leader_id = 4 WHERE id = 1;
UPDATE therapy_group SET leader_id = 6 WHERE id = 2;
UPDATE therapy_group SET leader_id = 7 WHERE id = 3;
UPDATE therapy_group SET leader_id = 10 WHERE id = 4;

-- ============================================================
-- 患者数据
-- ============================================================
INSERT INTO patient (id, name, gender, age, inpatient_no, bed_no, admission_date, diagnosis, allergy_history, contact_phone, emergency_contact, emergency_phone, attending_therapist_id, attending_doctor_id, status) VALUES
(1, '张三', 1, 58, 'INP20260001', 'A101', '2026-05-20', '脑卒中后左侧偏瘫，运动功能障碍', '青霉素过敏', '13900001001', '张妻', '13900001002', 4, 2, 'IN_HOSPITAL'),
(2, '李四', 0, 65, 'INP20260002', 'A102', '2026-05-22', '脊髓损伤T10水平，双下肢功能障碍', '无', '13900002001', '李夫', '13900002002', 4, 2, 'IN_HOSPITAL'),
(3, '王五', 1, 42, 'INP20260003', 'B201', '2026-05-25', '膝关节置换术后，关节活动受限', '磺胺类过敏', '13900003001', '王妻', '13900003002', 5, 3, 'IN_HOSPITAL'),
(4, '赵六', 0, 70, 'INP20260004', 'B202', '2026-05-28', '帕金森病，平衡功能障碍，步态异常', '无', '13900004001', '赵子', '13900004002', 6, 3, 'IN_HOSPITAL'),
(5, '孙七', 1, 35, 'INP20260005', 'C301', '2026-06-01', '肱骨骨折术后，肩关节功能障碍', '无', '13900005001', '孙母', '13900005002', 6, 2, 'IN_HOSPITAL'),
(6, '周八', 0, 55, 'INP20260006', 'C302', '2026-05-15', '脑外伤后认知功能障碍，言语障碍', '头孢过敏', '13900006001', '周夫', '13900006002', 7, 3, 'IN_HOSPITAL'),
(7, '吴九', 1, 48, 'INP20260007', 'D401', '2026-05-18', '腰椎间盘突出术后，腰背肌力减弱', '无', '13900007001', '吴妻', '13900007002', 10, 2, 'IN_HOSPITAL'),
(8, '郑十', 0, 62, 'INP20260008', 'D402', '2026-04-10', '脑卒中后言语障碍，吞咽功能障碍', '无', '13900008001', '郑女', '13900008002', 10, 3, 'DISCHARGED');

-- ============================================================
-- 量表模板
-- ============================================================
INSERT INTO assessment_template (id, template_name, abbreviation, category, items, scoring_rule, max_score, status) VALUES
(1, 'Fugl-Meyer运动功能评估', 'FMA', '运动功能',
   '[{"name":"上肢反射活动","max":6},{"name":"屈肌协同运动","max":12},{"name":"伸肌协同运动","max":10}]',
   '每项按0-1-2三级评分，总分越高功能越好', 66.00, 1),
(2, 'Barthel指数', 'BI', '日常生活能力',
   '[{"name":"进食","max":10},{"name":"洗澡","max":5},{"name":"修饰","max":5},{"name":"穿衣","max":10},{"name":"大便控制","max":10},{"name":"小便控制","max":10},{"name":"如厕","max":10},{"name":"床椅转移","max":15},{"name":"行走","max":15},{"name":"上下楼梯","max":10}]',
   '每项按依赖程度评分，总分100分', 100.00, 1),
(3, '改良Ashworth量表', 'MAS', '肌张力',
   '[{"name":"肩关节","max":4},{"name":"肘关节","max":4},{"name":"腕关节","max":4},{"name":"膝关节","max":4}]',
   '0-4级，0级正常，4级僵硬', 16.00, 1),
(4, 'Berg平衡量表', 'BBS', '平衡功能',
   '[{"name":"坐位站起","max":4},{"name":"无支撑站立","max":4},{"name":"无支撑坐位","max":4},{"name":"站到坐","max":4}]',
   '每项0-4分，总分56分，<40分有跌倒风险', 56.00, 1),
(5, '视觉模拟评分', 'VAS', '疼痛',
   '[{"name":"疼痛程度","max":10}]',
   '0分无痛，10分剧痛，患者自评', 10.00, 1);

-- ============================================================
-- 系统字典
-- ============================================================
INSERT INTO system_dict (dict_type, dict_code, dict_value, sort_order) VALUES
-- 治疗项目类型
('TREATMENT_TYPE', 'PT', '物理治疗', 1),
('TREATMENT_TYPE', 'OT', '作业治疗', 2),
('TREATMENT_TYPE', 'ST', '言语治疗', 3),
('TREATMENT_TYPE', 'MT', '手法治疗', 4),
('TREATMENT_TYPE', 'ET', '运动治疗', 5),
('TREATMENT_TYPE', 'ELEC', '物理因子治疗', 6),
-- 医嘱类型
('ORDER_TYPE', 'ROUTINE', '常规医嘱', 1),
('ORDER_TYPE', 'TEMP', '临时医嘱', 2),
('ORDER_TYPE', 'LONG_TERM', '长期医嘱', 3),
-- 治疗频次
('FREQUENCY', 'QD', '每日1次', 1),
('FREQUENCY', 'BID', '每日2次', 2),
('FREQUENCY', 'TID', '每日3次', 3),
('FREQUENCY', 'QOD', '隔日1次', 4),
('FREQUENCY', 'QW', '每周1次', 5),
('FREQUENCY', 'BIW', '每周2次', 6);
