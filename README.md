# 康复科管理系统

面向医院康复科的数字化管理平台，覆盖「评估 → 方案制定 → 医嘱下达 → 患者日程排程 → 任务执行 → 核销结算 → 工作量统计」完整业务闭环。

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.3.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 安全 | Spring Security + JWT | 0.12.6 |
| 缓存 | Redis | 7.x |
| 实时通信 | WebSocket (STOMP) | — |
| API 文档 | Knife4j (Swagger) | 4.5.0 |
| 定时任务 | Spring @Scheduled | — |
| Excel | EasyExcel | 3.3.4 |
| 数据库 | MySQL | 8.0+ |
| 前端框架 | Vue 3 (Composition API) | 3.5 |
| UI 组件库 | Element Plus | 2.14 |
| 状态管理 | Pinia | 3.0 |
| 图表 | ECharts | 6.1 |
| 构建工具 | Vite | 8.0 |
| 语言 | TypeScript | 6.0 |

## 功能模块

### 治疗师端 / 护士端
- **治疗任务** — 日历视图、任务创建/开始/核销/撤销、状态筛选
- **患者列表** — 多维度筛选（我的/本组/全部）、关键词搜索、9项操作入口
- **患者详情** — 基本信息、量表评估、治疗目标、治疗方案、医嘱查看、患者日程、治疗记录、出院办理
- **工作量统计** — 今日/本周/本月汇总卡片、治疗趋势图表、每日明细
- **功能广场** — 快捷导航入口、量表评估、我的日程、消息通知

### 医生端
- **医嘱管理** — 创建/编辑/审核通过/退回/作废医嘱
- **排程生成任务** — 审核通过后批量生成治疗任务
- **患者列表** — 查看全部患者及关联医嘱

### 管理员端
- **用户管理** — 治疗师/护士/医生/管理员 CRUD、状态启停、密码重置
- **小组管理** — 治疗小组 CRUD、组长分配
- **字典管理** — 系统枚举值维护
- **操作日志** — 审计日志查询
- **数据统计** — 全科数据看板、各角色工作量、治疗类型分布、每日核销趋势

## 项目结构

```
healthmanager/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/com/rehab/
│   │   ├── common/                   # 公共类 (Result, PageResult, 异常处理)
│   │   ├── config/                   # 配置 (Security, JWT, CORS, Redis, WebSocket)
│   │   ├── infrastructure/           # 基础设施 (JWT工具, 用户上下文)
│   │   └── module/
│   │       ├── assessment/           # 量表评估
│   │       ├── auth/                 # 认证授权
│   │       ├── order/                # 医嘱管理
│   │       ├── patient/              # 患者管理
│   │       ├── schedule/             # 排程 & 定时任务
│   │       ├── system/               # 系统管理
│   │       ├── task/                 # 治疗任务
│   │       ├── treatment/            # 治疗方案 & 记录
│   │       ├── websocket/            # 实时推送
│   │       └── workload/             # 工作量统计
│   └── src/test/                     # 测试 (JUnit 5 + Mockito + H2)
│
├── frontend/                         # Vue 3 前端
│   └── src/
│       ├── api/                      # API 接口 & 类型定义
│       ├── composables/              # WebSocket 组合式函数
│       ├── layout/                   # 布局组件 (侧边栏/顶栏)
│       ├── router/                   # 路由配置 (角色权限守卫)
│       ├── stores/                   # Pinia 状态管理
│       └── views/
│           ├── therapist/            # 治疗师端 (任务/患者/工作量/广场)
│           ├── doctor/               # 医生端 (医嘱/患者)
│           ├── admin/                # 管理员端 (用户/小组/字典/日志/统计)
│           ├── login/                # 登录
│           └── settings/             # 个人设置
│
└── sql/                              # 数据库脚本
    ├── 01_schema.sql                 # 建表 (15张表)
    ├── 02_init_data.sql              # 初始数据
    └── 03_notification.sql           # 通知表
```

## 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Redis 7.x
- Node.js 20+

### 后端启动

```bash
cd backend

# 1. 创建数据库并导入表结构
mysql -u root -p < ../sql/01_schema.sql
mysql -u root -p < ../sql/02_init_data.sql
mysql -u root -p < ../sql/03_notification.sql

# 2. 修改数据库连接配置
vim src/main/resources/application-dev.yml

# 3. 启动
mvn spring-boot:run
```

API 文档：http://localhost:8080/doc.html

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问 http://localhost:5173

### 测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | rehab123 |
| 医生 | doctor1 | rehab123 |
| 治疗师 | therapist1 | rehab123 |
| 护士 | nurse1 | rehab123 |

## API 接口

所有接口前缀 `/api/v1`，JWT Bearer Token 认证。

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /auth/login | 登录 |
| GET | /auth/userinfo | 获取当前用户信息 |
| PUT | /auth/password | 修改密码 |

### 患者管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /patients | 患者列表 (keyword/status/viewScope/page/size) |
| GET | /patients/{id} | 患者详情 |
| POST | /patients | 新增患者 |
| PUT | /patients/{id} | 编辑患者 |
| PUT | /patients/{id}/discharge | 办理出院 |
| POST | /patients/import | Excel 批量导入 |
| GET | /patients/export | Excel 导出 |

### 治疗任务
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /tasks | 任务列表 (therapistId/groupId/patientId/status/date) |
| POST | /tasks | 创建任务 |
| GET | /tasks/calendar | 日历视图 (year/month) |
| POST | /tasks/{id}/start | 开始执行 |
| POST | /tasks/{id}/verify | 核销确认 |
| POST | /tasks/{id}/revoke | 撤销核销 |

### 医嘱管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /orders | 医嘱列表 |
| POST | /orders | 创建医嘱 |
| PUT | /orders/{id} | 编辑医嘱 |
| PUT | /orders/{id}/approve | 审核通过 |
| PUT | /orders/{id}/reject | 审核退回 |
| PUT | /orders/{id}/cancel | 作废 |
| POST | /orders/{id}/generate-tasks | 生成治疗任务 |

### 量表评估
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /assessments | 评估列表 (patientId) |
| POST | /assessments | 创建评估 |
| PUT | /assessments/{id} | 修改评估 |
| DELETE | /assessments/{id} | 删除评估 |
| GET | /assessments/templates | 评估模板列表 |

### 治疗方案
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /treatment-plans | 方案列表 (patientId) |
| POST | /treatment-plans | 创建方案 |
| PUT | /treatment-plans/{id} | 修改方案 |
| DELETE | /treatment-plans/{id} | 删除方案 |
| POST | /treatment-plans/{id}/submit | 提交审阅 |

### 患者日程
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /patients/{id}/schedule | 患者日程 (startDate/endDate) |
| GET | /schedule/my | 我的日程 |
| GET | /schedule/group | 本组日程 |
| POST | /schedule/{id}/generate-task | 从日程生成任务 |

### 工作量统计
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /workload/personal | 个人统计 |
| GET | /workload/group | 小组统计 |
| GET | /workload/department | 全科统计 |
| GET | /workload/trend | 趋势数据 |
| GET | /workload/export | Excel 导出 |

### 系统管理（管理员）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | /admin/users | 用户管理 |
| GET/POST/PUT/DELETE | /admin/therapy-groups | 小组管理 |
| GET/POST/PUT/DELETE | /admin/dicts | 字典管理 |
| GET | /admin/logs | 操作日志 |
| GET | /admin/stats | 系统统计 |

### 通知
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /notifications | 通知列表 |
| PUT | /notifications/{id}/read | 标记已读 |
| PUT | /notifications/read-all | 全部已读 |
| GET | /notifications/unread-count | 未读数量 |

## 数据库

15 张核心表：`therapy_group`, `user`, `patient`, `assessment_template`, `assessment`, `treatment_goal`, `treatment_plan`, `medical_order`, `task`, `task_verification`, `workload_stat`, `treatment_record`, `patient_schedule`, `operation_log`, `system_dict`, `notification`

全部使用软删除（is_deleted）、乐观锁（version）、逻辑索引。

## 业务流程

```
量表评估 → 治疗目标 → 治疗方案 → 提交审阅
                                    ↓
                              医生审核方案
                                    ↓
                              下达医嘱 (审核通过)
                                    ↓
                              生成患者日程条目
                                    ↓
                         治疗师查看日程 + 排程
                                    ↓
                         批量生成治疗任务 (按频次)
                                    ↓
                         执行治疗 → 核销确认
                                    ↓
                         工作量统计 + 治疗记录
```

## 定时任务

| 任务 | Cron | 说明 |
|------|------|------|
| 每日任务提醒 | 每天 07:30 | 通知治疗师今日待处理任务数量 |
| 自动生成任务 | 每天 06:00 | 为当日有效医嘱自动生成任务 |
| 月度工作量汇总 | 每月 1 日 02:00 | 汇总上月各人工作量并发送通知 |

## 运行测试

```bash
cd backend
mvn test -Dnet.bytebuddy.experimental=true
```

## License

MIT
