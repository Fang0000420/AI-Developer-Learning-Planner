# AI Developer Learning Planner

面向程序员的 AI 学习与技术成长规划 Agent 系统。

## 项目定位

AI Developer Learning Planner 是一个面向程序员、研究生和技术转型者的 AI Agent 系统。用户输入技术背景、当前技能、学习目标、求职目标、每天可投入时间和计划周期后，系统会分析用户能力画像，拆解学习目标，识别技能差距，推荐项目方向，生成每日学习与实践任务，并根据用户每天提交的完成情况动态调整后续计划。

项目重点不是一次性生成静态学习计划，而是构建一个可持续迭代的工程闭环：多 Agent 协作、有状态 workflow、数据库持久化、每日任务管理、进度反馈和动态计划调整。

## MVP 闭环

MVP 阶段优先跑通从目标输入到计划调整的完整链路：

1. 用户填写技术背景、已有技能、目标方向、每日可投入时间和计划周期。
2. Agent 服务生成用户能力画像。
3. 系统拆解目标并识别技能差距。
4. Agent 服务推荐适合的学习项目方向。
5. 系统生成 14 天或 21 天每日任务计划。
6. 用户每天提交任务完成情况和复盘说明。
7. Agent 服务评估当日进度，给出反馈。
8. 系统根据反馈动态调整后续任务。

## 当前 Demo 路径

当前已完成 Day 18，可以演示一条带基础登录保护、异步 job 和 Agent 执行追踪的完整 MVP 闭环：

1. 打开前端 `/login`，注册或登录一个测试用户。
2. 进入 `New Goal` 创建目标。
3. 推荐 demo 输入：Java 后端开发背景，有 Python 和基础 AI 使用经验，目标是在 21 天内做出 AI Developer Learning Planner，每天 2 小时。
4. 在目标详情页点击侧栏 `Run to Plan`，系统会补齐能力画像、目标拆解、技能差距分析、项目推荐，并通过异步 job 生成学习计划。
5. 打开生成的计划详情，进入 `Today` 或 Day 1 任务页。
6. 勾选部分任务为完成、至少保留一个未完成任务，填写反馈和阻塞项后提交进度。
7. 最近提交记录会展示 Progress Reviewer 的 impact、suggestion 和 blockers。
8. 切到 Day 2，确认 Plan Adjuster 新增的 carry-over 或 split 任务。
9. 打开顶部导航 `Agent Runs`，查看本次 workflow 的 Agent 执行历史；进入详情页可检查 input/output JSON、latency、status、errorMessage 和 requestId。

服务器验收时可同步检查数据库：`async_jobs` 中应有 `PLAN_GENERATION` 和 `PROGRESS_SUBMISSION` 的 `SUCCEEDED` 记录；`agent_runs` 中应有 `Profile Analyzer`、`Goal Decomposer`、`Skill Gap Analyzer`、`Project Recommender`、`Plan Generator`、`Progress Reviewer` 和 `Plan Adjuster` 的 `SUCCESS` 记录；`agent_runs.request_id` 应有值，`Plan Generator`、`Progress Reviewer`、`Plan Adjuster` 等可关联计划的记录应有 `plan_id`；`skill_profiles`、`learning_plans`、`daily_tasks` 和 `progress_logs.review_result_json` 应能追踪对应结果。

## 技术栈

| 模块              | 技术                                                                                                                                        |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| 前端              | Next.js, React, TypeScript, Tailwind CSS, shadcn/ui, React Hook Form, Zod, TanStack Query                                                   |
| Java 后端         | Spring Boot, Spring Web, Spring Security, Spring Data JPA, Hibernate, Flyway, Springdoc OpenAPI, MapStruct, Lombok, JUnit 5, Testcontainers |
| Python Agent 服务 | FastAPI, Pydantic, DeepSeek/OpenAI-compatible API, Pytest, Ruff                                                                                 |
| 数据库            | PostgreSQL                                                                                                                                  |
| 缓存与异步队列    | Redis                                                                                                                                       |
| 基础设施          | Docker Compose, GitHub Actions                                                                                                              |

## 服务拆分

| 服务          | 目录             | 端口   | 职责                                                                             |
| ------------- | ---------------- | ------ | -------------------------------------------------------------------------------- |
| Frontend      | `frontend/`      | `3000` | 用户输入、计划展示、每日任务展示、进度提交和 Agent 结果展示                      |
| Backend       | `backend/`       | `8080` | 用户、目标、学习计划、每日任务、进度日志和 Agent 执行记录管理，对外提供 REST API |
| Agent Service | `agent-service/` | `8000` | 能力画像分析、目标拆解、技能差距分析、项目推荐、计划生成、进度复盘和计划调整     |
| PostgreSQL    | `infra/`         | `5432` | 核心业务数据持久化                                                               |
| Redis         | `infra/`         | `6379` | 异步任务队列、缓存和任务状态辅助存储                                             |

## 目录结构

```text
.
├── frontend/           # Next.js frontend application
├── backend/            # Spring Boot backend service
├── agent-service/      # FastAPI AI agent service
├── infra/              # Docker Compose and infrastructure configuration
├── docs/               # Architecture notes and project documentation
└── .github/workflows/  # GitHub Actions workflows
```

## 启动与验收

当前仓库处于 Day 18 日志与可观测性阶段。`backend/` 已具备基础注册登录、JWT 鉴权、goals CRUD、Agent 编排、计划生成、每日任务、进度提交、进度复盘、计划调整、异步 job 状态接口和 Agent Runs 查询接口；`agent-service/` 已具备 profile、goal decomposition、skill gap、project recommendation、plan generation、progress review 和 plan adjustment 接口，并对真实模型调用增加 requestId 日志和错误分类重试。未配置 `DEEPSEEK_API_KEY` 时 Agent 服务使用 mock fallback；已配置真实模型时，网络、超时、429、5xx 和模型输出格式问题会先重试，重试耗尽后才 fallback，401、403、404、400 等配置或权限问题会直接报错。

默认验收环境为服务器 `/home/AI-Developer-Learning-Planner`。启动或重启服务前，先在项目根目录加载 `.env`：

```bash
set -a && source .env && set +a
```

基础启动方式：

```bash
# 1. PostgreSQL 和 Redis
docker compose -f infra/docker-compose.yml up postgres redis

# 2. Java 后端
cd backend
mvn spring-boot:run

# 3. 后端健康检查
curl http://localhost:8080/api/health

# 4. Python Agent 服务
cd agent-service
source .venv/bin/activate
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# 5. Agent 健康检查
curl http://localhost:8000/health

# 6. 前端
cd frontend
npm run build
npm run start:server
```

## 工程约定

| 项目                 | 约定                    |
| -------------------- | ----------------------- |
| 默认前端地址         | `http://localhost:3000` |
| 默认后端地址         | `http://localhost:8080` |
| 默认 Agent 服务地址  | `http://localhost:8000` |
| 默认 PostgreSQL 地址 | `localhost:5432`        |
| 默认 Redis 地址      | `localhost:6379`        |

环境变量约定见 `.env.example`，基础架构说明见 `docs/architecture.md`。各服务初始化会在后续任务中继续完善。
