# AGENTS.md

面向后续 coding agent 的项目交接与阶段记录。本文档只保留当前状态、关键约定、已完成阶段摘要和仍需注意的坑点；每日流水细节可回看 `Plan/` 或 Git 历史。

## 当前阶段

- 当前日期: 2026-06-10
- 当前进度: Day 18 已完成
- 当前主题: 日志与可观测性
- 下一阶段: Day 19 - 测试与 CI
- 当前模型策略: 能力画像、目标拆解、技能差距分析、项目推荐、计划生成、进度复盘和计划调整优先使用 DeepSeek，目标模型暂定 `deepseek-v4-pro`；未配置 `DEEPSEEK_API_KEY` 时 Agent 服务使用 mock fallback；已配置真实模型时会区分可重试错误和不可重试错误，网络/超时/429/5xx/模型输出格式问题先重试，重试耗尽后才 fallback，401/403/404/400 等配置或权限问题直接报错不 fallback；服务器暂不部署 Clash 或其他代理方案。

Day 13 已完成并在服务器验收通过：Agent 服务新增 `POST /agent/progress/review`，可根据今日任务、完成/未完成任务、阻塞项和用户反馈返回结构化复盘；后端 `POST /api/progress` 提交后会调用 Progress Reviewer，将结果保存到 `progress_logs.review_result_json`，并把本次调用写入 `agent_runs`；前端 `/plans/[planId]/today` 的最近提交记录可展示 impact、suggestion 和 reviewer blockers。服务器数据库已确认 `progress_logs.review_result_json` 包含复盘内容，`agent_runs` 已写入 `Progress Reviewer` 的 `SUCCESS` 记录。

Day 14 已完成并在服务器验收通过：Agent 服务新增 `POST /agent/plan/adjust`，可根据当前计划、今日任务、Progress Reviewer 结果、未完成任务和明日任务返回局部调整方案；后端 `POST /api/progress` 在复盘成功后会调用 Plan Adjuster，将未完成任务以 carry-over 或拆分子任务形式追加到后续日期，保留当天任务作为进度记录，并将调整结果写入 `progress_logs.review_result_json.planAdjustment` 和 `learning_plans.plan_json.adjustmentHistory`，同时记录 `Plan Adjuster` 的 `agent_runs`；前端 `/plans/[planId]/today` 的最近提交记录可展示 Tomorrow adjustment、moved tasks 和 split tasks。服务器已确认 `agent_runs` 写入 `Plan Adjuster` 的 `SUCCESS` 记录，`progress_logs.review_result_json.planAdjustment` 包含调整原因和 moved tasks，`learning_plans.plan_json.adjustmentHistory` 已追加历史，`daily_tasks` 后续日期已新增 carry-over 任务。

Day 15 已完成并在服务器验收通过：未新增后端或 Agent 大功能，重点收口可演示主链路。前端目标详情页新增侧栏 `Demo Chain` / `Run to Plan`，可补齐缺失的 Profile Analyzer、Goal Decomposer、Skill Gap Analyzer 和 Project Recommender 结果，并生成 Learning Plan 后跳转计划详情；首页更新为 Day 15 demo flow 文案；README 新增当前 demo 路径、推荐输入和数据库核查点。服务器已按完整闭环路径完成验收：创建目标、运行 `Run to Plan`、生成计划、提交 Day 1 进度、展示 Progress Reviewer 复盘，并确认 Day 2 出现 Plan Adjuster 新增的 carry-over 或 split 任务。前端本机验证 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 均通过。

Day 16 已完成并在服务器验收通过：后端新增 `async_jobs` 表和 `GET /api/jobs/{jobId}` 状态查询，新增 `POST /api/jobs/plan-generation` 与 `POST /api/jobs/progress-submission` 用后台线程执行计划生成、进度复盘和计划调整，完成后仍复用原服务保存 `learning_plans`、`daily_tasks`、`progress_logs` 和 `agent_runs`；前端 `Run to Plan`、`Generate Learning Plan` 和每日进度提交改为发起 job 并轮询 `PENDING/RUNNING/SUCCEEDED/FAILED`，成功后跳转计划页或刷新今日任务页。服务器已确认异步计划生成和异步进度提交测试通过，`async_jobs` 可记录 `PLAN_GENERATION` 和 `PROGRESS_SUBMISSION` 的成功状态，计划、任务、复盘和计划调整仍正确落表。MVP 阶段暂未引入 Celery worker，避免调整服务器部署链路；Redis/Celery 可留到后续需要跨进程队列和重试机制时补齐。本机验证：后端临时指定 Java 17 后 `mvn test` 通过，前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 均通过。

Day 17 已完成并在服务器验收通过：后端新增 `POST /api/auth/register` 和 `POST /api/auth/login`，使用 BCrypt 保存密码并返回 HMAC JWT；Spring Security 默认保护除 health/auth/docs 外的后端接口，goals、plans、tasks、progress、jobs 及各类 goal agent 结果接口会按当前 JWT 用户过滤或校验资源归属，越权资源按 404 处理；`async_jobs` 新增 `user_id` 绑定 job 查询权限。前端新增 `/login` 注册/登录页，同源 auth route 会把 JWT 写入 `ai_planner_token` cookie，服务端页面请求和所有同源 API proxy 会自动转发 Bearer token，顶部导航可显示登录用户名并支持退出。Agent 服务新增统一 `retry_model_call`，真实模型调用在 HTTP、JSON 解析或 Pydantic 校验失败时最多尝试 3 次，仍保留原 mock fallback 策略。服务器验收时修复了 `SecurityConfig` 少分号导致后端编译失败的问题，并修复了 HTTP 部署下 cookie 被错误设置为 `Secure` 导致浏览器不保存 token 的问题；最终确认登录后 goals/plans 请求会带 token，页面可显示登录用户名。本机验证：后端临时指定 Java 17 后 `mvn test` 通过；Agent `pytest` 和 `ruff check .` 通过；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 均通过。

Day 18 已完成并在服务器验收通过：后端新增 `V9__add_agent_run_observability_fields.sql`，为 `agent_runs` 增加 `plan_id`、`request_id` 和相关索引；新增 `GET /api/agent-runs` 与 `GET /api/agent-runs/{runId}`，支持按 `goalId`、`planId`、`agentName` 过滤，并按当前 JWT 用户限制访问，详情返回 input/output JSON、错误原因、latency 和 requestId。后端新增请求日志过滤器，为每个请求生成或透传 `X-Request-Id`，响应回写同名 header，日志 pattern 写入 MDC requestId；所有 Spring 到 Agent 服务的 HTTP client 会透传 requestId，异步 job 会把创建请求的 requestId 带入后台线程；Agent 服务新增 FastAPI request logging middleware，回传 `X-Request-Id`，模型重试日志也包含 requestId。前端新增 `/agent-runs` 和 `/agent-runs/[runId]` 页面，以及同源 `/api/agent-runs`、`/api/agent-runs/[runId]` route，顶部导航新增 Agent Runs 入口。服务器验收时通过 Agent Runs 页面定位到一次 Goal Decomposer 偶发失败，随后补齐 Goal Decomposer 模型输出归一化，并将 Agent 服务模型调用改为按错误类型处理：可重试错误重试耗尽后 fallback，不可重试错误直接报错。本机验证：后端临时指定 Java 17 后 `mvn test` 通过，`Tests run: 89, Failures: 0, Errors: 0, Skipped: 0`；Agent `.venv` 下 `pytest` 通过，`28 passed, 1 warning`，`ruff check .` 通过；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 均通过。

## 运行环境约定

所有每日任务的开发、启动、测试和验收默认在服务器 `/home/AI-Developer-Learning-Planner` 中执行。本机只用于计划文件编辑、必要的 Git 辅助操作和与 agent 协作，不作为项目服务运行环境。

后续更新 `Plan/` 中的每日任务时，应继续保持服务器运行口径，验收标准也应以服务器执行结果为准。

## 项目定位

AI Developer Learning Planner 是一个面向程序员、研究生和技术转型者的 AI 学习与技术成长规划 Agent 系统。MVP 目标是跑通以下闭环：

1. 用户输入背景、技能、目标和计划周期。
2. Agent 服务生成能力画像和目标拆解。
3. 系统识别技能差距并推荐项目方向。
4. 系统生成 14 天或 21 天每日任务计划。
5. 用户每天提交完成情况。
6. Agent 服务复盘进度并动态调整后续任务。

## 仓库结构

```text
.
├── AGENTS.md
├── README.md
├── .env.example
├── frontend/
├── backend/
├── agent-service/
├── infra/
├── docs/
└── .github/workflows/
```

## 服务与端口

| 服务 | 目录 | 默认地址 | 端口 |
| --- | --- | --- | --- |
| Frontend | `frontend/` | `http://localhost:3000` | `3000` |
| Backend | `backend/` | `http://localhost:8080` | `8080` |
| Agent Service | `agent-service/` | `http://localhost:8000` | `8000` |
| PostgreSQL | `infra/` | `localhost:5432` | `5432` |
| Redis | `infra/` | `localhost:6379` | `6379` |

环境变量命名以 `.env.example` 为准，架构说明以 `docs/architecture.md` 为准。

## 关键接口

### Backend

- `GET /api/health`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/goals`
- `GET /api/goals`
- `GET /api/goals/{goalId}`
- `PUT /api/goals/{goalId}`
- `DELETE /api/goals/{goalId}`
- `GET /api/goals/{goalId}/profile`
- `POST /api/goals/{goalId}/profile/analyze`
- `GET /api/goals/{goalId}/decomposition`
- `POST /api/goals/{goalId}/decomposition/decompose`
- `GET /api/goals/{goalId}/skill-gap`
- `POST /api/goals/{goalId}/skill-gap/analyze`
- `GET /api/goals/{goalId}/project-recommendation`
- `POST /api/goals/{goalId}/project-recommendation/recommend`
- `POST /api/plans/generate`
- `POST /api/jobs/plan-generation`
- `POST /api/jobs/progress-submission`
- `GET /api/jobs/{jobId}`
- `GET /api/agent-runs?goalId={goalId}&planId={planId}&agentName={agentName}`
- `GET /api/agent-runs/{runId}`
- `GET /api/plans`
- `GET /api/plans/{planId}`
- `PUT /api/plans/{planId}`
- `DELETE /api/plans/{planId}`
- `GET /api/plans/{planId}/tasks`
- `GET /api/plans/{planId}/tasks/today?dayIndex={dayIndex}`
- `PUT /api/plans/{planId}/tasks/{taskId}/status`
- `POST /api/progress`
- `GET /api/progress/{planId}?dayIndex={dayIndex}`

### Agent Service

- `GET /health`
- `POST /agent/profile/analyze`
- `POST /agent/goal/decompose`
- `POST /agent/skill-gap/analyze`
- `POST /agent/project/recommend`
- `POST /agent/plan/generate`
- `POST /agent/plan/adjust`
- `POST /agent/progress/review`

### Frontend 同源 API Route

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/backend-health`
- `GET/POST /api/goals`
- `GET/PUT/DELETE /api/goals/[goalId]`
- `GET /api/goals/[goalId]/profile`
- `POST /api/goals/[goalId]/profile/analyze`
- `GET /api/goals/[goalId]/decomposition`
- `POST /api/goals/[goalId]/decomposition/decompose`
- `GET /api/goals/[goalId]/skill-gap`
- `POST /api/goals/[goalId]/skill-gap/analyze`
- `GET /api/goals/[goalId]/project-recommendation`
- `POST /api/goals/[goalId]/project-recommendation/recommend`
- `POST /api/plans/generate`
- `POST /api/jobs/plan-generation`
- `POST /api/jobs/progress-submission`
- `GET /api/jobs/[jobId]`
- `GET /api/agent-runs?goalId={goalId}&planId={planId}&agentName={agentName}`
- `GET /api/agent-runs/[runId]`
- `GET /api/plans`
- `GET /api/plans/[planId]`
- `PUT /api/plans/[planId]`
- `DELETE /api/plans/[planId]`
- `GET /api/plans/[planId]/tasks`
- `GET /api/plans/[planId]/tasks/today?dayIndex={dayIndex}`
- `PUT /api/plans/[planId]/tasks/[taskId]/status`
- `POST /api/progress`
- `GET /api/progress/[planId]?dayIndex={dayIndex}`

前端通过 Next.js 同源 API Route 请求后端，避免公网浏览器直接访问服务器自身 `localhost:8080`。

## 数据库当前状态

已落地 Flyway migrations：

- `V1__create_users_and_goals.sql`: `users`, `goals`
- `V2__create_skill_profiles_and_agent_runs.sql`: `skill_profiles`, `agent_runs`
- `V3__create_learning_plans_and_daily_tasks.sql`: `learning_plans`, `daily_tasks`
- `V4__add_learning_plan_status.sql`: 为 `learning_plans` 增加 `status`
- `V5__rename_completed_daily_task_status.sql`: 将历史 daily task 状态 `COMPLETED` 统一迁移为 `DONE`
- `V6__create_progress_logs.sql`: `progress_logs`
- `V7__create_async_jobs.sql`: `async_jobs`
- `V8__add_async_job_user_id.sql`: 为 `async_jobs` 增加 `user_id`
- `V9__add_agent_run_observability_fields.sql`: 为 `agent_runs` 增加 `plan_id`、`request_id` 和查询索引

Day 17 开始接入基础登录；未登录请求不能访问受保护后端接口。前端通过 `/login` 注册或登录后，将 JWT 保存到 `ai_planner_token` cookie 并由同源 API Route 自动转发。创建目标时仍兼容 `userId` 可选字段；已登录时以后端当前 JWT 用户为准，未登录兼容路径仍会自动使用或创建 `demo-user`。

Day 07 目标拆解、Day 08 技能差距分析和 Day 09 项目推荐暂不新增专表；最新结果从 `agent_runs` 中按 `goal_id`、对应 `agent_name` 和 `status = 'SUCCESS'` 查询，并解析 `output_json` 返回前端。当前使用的 `agent_name` 包括 `Goal Decomposer`、`Skill Gap Analyzer`、`Project Recommender`、`Plan Generator`、`Progress Reviewer` 和 `Plan Adjuster`。Day 10 开始学习计划正式落表：`learning_plans` 保存计划标题、周期、状态、完整 `plan_json` 和来源 agent run；`daily_tasks` 保存按天拆分的任务、预计时长、类型、交付物、优先级和状态。Day 11 开始每日任务支持独立页面查询和状态更新；`daily_tasks.status` 当前约定为 `PENDING`、`IN_PROGRESS`、`DONE`、`SKIPPED`。Day 12 开始用户每日进度正式落表：`progress_logs` 保存计划、目标、用户、`day_index`、`user_feedback`、`completed_task_ids`、`unfinished_task_ids`、`blockers` 和 `review_result_json`；Day 13 开始 `review_result_json` 保存 Progress Reviewer 的结构化输出，字段为 `completedTasks`、`unfinishedTasks`、`blockers`、`impact` 和 `suggestion`；Day 14 开始 `review_result_json.planAdjustment` 保存 Plan Adjuster 的局部调整结果，`learning_plans.plan_json.adjustmentHistory` 保存调整历史；Day 16 开始 `async_jobs` 保存异步计划生成和异步进度提交的 job 类型、状态、输入、结果和错误信息；Day 17 开始 `async_jobs.user_id` 绑定 job 创建者，登录后只能查询自己的 job；Day 18 开始 `agent_runs.plan_id` 绑定可关联计划的 Agent 调用，`agent_runs.request_id` 保存请求链路 ID，用于关联前端代理、后端和 Agent 服务日志。

## 环境变量重点

- `NEXT_PUBLIC_BACKEND_API_BASE_URL=http://localhost:8080`
- `AGENT_SERVICE_BASE_URL=http://localhost:8000`
- `JWT_SECRET=replace-with-a-long-random-secret`
- `JWT_EXPIRATION_SECONDS=86400`
- `DEEPSEEK_API_KEY=`
- `DEEPSEEK_API_BASE_URL=https://api.deepseek.com`
- `PROFILE_ANALYZER_MODEL=deepseek-v4-pro`
- `PROFILE_ANALYZER_TIMEOUT_SECONDS=30`

真实 `.env` 和任何密钥不能提交。未配置 `DEEPSEEK_API_KEY` 时，Agent 服务会使用 mock fallback，不请求外部模型。

## 进度摘要

### Day 01 - 项目初始化 - 已完成

- 建立 monorepo 结构、README、`.env.example`、`infra/docker-compose.yml`、`docs/architecture.md` 和 GitHub 仓库。
- 服务器已拉取代码，Docker Compose 配置解析通过。

### Day 02 - Spring Boot 后端初始化 - 已完成

- `backend/` 初始化为 Maven + Spring Boot 3.4.2 + Java 17 工程。
- 完成 PostgreSQL/JPA/Flyway 配置、`GET /api/health`、`users`/`goals` migration、JPA Entity/Repository 和基础测试。
- 服务器验证：`mvn test` 通过，health 接口返回 `UP`，数据库确认 `flyway_schema_history`、`goals`、`users` 存在。

### Day 03 - FastAPI Agent 服务初始化 - 已完成

- `agent-service/` 初始化为 Python + FastAPI 工程。
- 完成依赖固定、`GET /health`、`POST /agent/profile/analyze` 假数据接口、Pydantic schema、service 分层和基础 pytest/ruff 配置。
- 服务器曾使用 `8001` 临时端口验证 `/health` 和 `/agent/profile/analyze` 通过；后续默认端口仍以 `.env.example` 的 `8000` 为准。

### Day 04 - Next.js 前端初始化 - 已完成

- `frontend/` 初始化为 Next.js 16 + React 19 + TypeScript + Tailwind CSS 4 工程。
- 完成基础工作台首页、顶部导航、`/goals/new` 目标输入页、`/api/backend-health` 后端 health 代理和前端 README 说明。
- 本机验证：`npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：公网首页和 `/goals/new` 可访问，首页 `Backend Status` 显示 Online。

### Day 05 - 目标管理模块 - 已完成

- 后端完成 goals CRUD、DTO、Service、Controller、统一 400/404 错误响应和基础测试。
- 前端完成 `/goals/new` 提交到后端、`/goals` 目标列表、`/goals/[goalId]` 目标详情，以及 `/api/goals`、`/api/goals/[goalId]` 同源代理。
- 本机验证：后端 `mvn test` 通过；前端 lint/typecheck/format/build 通过；浏览器用临时 8080 stub 验证目标创建、列表、详情均通过。

### Day 06 - Profile Analyzer - 已完成

- Agent 服务新增 JSON-only Profile Analyzer prompt、DeepSeek/OpenAI-compatible `/chat/completions` 调用路径、Pydantic 输出校验和 mock fallback。
- 后端新增 `skill_profiles`、`agent_runs` migration、JPA Entity/Repository、Agent client、画像分析 Service/Controller 和统一 502 错误响应。
- 前端目标详情页新增 `Skill Profile` 区域，支持生成中、成功、失败、重新生成，并展示 `currentSkills`、`strengths`、`weaknesses`、`recommendedDirection`。
- 本机验证：后端 Java 17 `mvn test` 通过，`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`；前端 lint/typecheck/format/build 通过；Agent `python -m compileall app` 通过。
- 服务器验收：DeepSeek API 可用；`POST /api/goals/1/profile/analyze` 成功；`skill_profiles` 和 `agent_runs` 已写入数据库；前端目标详情页可读取并展示画像。

### Day 07 - Goal Decomposer - 已完成

- Agent 服务新增 `Goal Decomposer` JSON-only prompt、`GoalDecomposeRequest/Response` schema、`POST /agent/goal/decompose` 接口、Pydantic 输出校验和 mock fallback；输出 `subGoals`，每项包含 `title`、`description`、`priority`。
- 后端新增 Goal Decomposition client/service/controller，复用 `agent_runs` 持久化输入输出和成功/失败状态；新增 `GET /api/goals/{goalId}/decomposition` 查询最新成功拆解，新增 `POST /api/goals/{goalId}/decomposition/decompose` 触发重新生成。
- 前端目标详情页新增 `Goal Decomposition` 区域，支持生成中、成功、失败、重新生成，并按优先级展示子目标列表；新增对应 Next.js 同源 API Route。
- 本机验证：Agent `.venv` 下 `pytest` 通过，`4 passed, 1 warning`；Agent `ruff check .` 通过；后端临时 Java 17 `mvn test` 通过，`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：`POST /agent/goal/decompose` 可返回结构化 `subGoals`；`POST /api/goals/1/decomposition/decompose` 成功返回 `runId`、`goalId` 和子目标列表；`GET /api/goals/1/decomposition` 可读取最新成功拆解；数据库 `agent_runs` 已写入 `Goal Decomposer` 的 `SUCCESS` 记录；前端目标详情页 `Goal Decomposition` 区域显示 `Ready` 并展示子目标列表。
- 已知小问题：`POST /api/goals/{goalId}/decomposition/decompose` 当次响应中 `createdAt` 可能为 `null`，随后通过 `GET /api/goals/{goalId}/decomposition` 可读到数据库生成的时间戳；当前前端不依赖该字段，可后续顺手优化。

### Day 08 - Skill Gap Analyzer - 已完成

- Agent 服务新增 `Skill Gap Analyzer` JSON-only prompt、`SkillGapAnalyzeRequest/Response` schema、`POST /agent/skill-gap/analyze` 接口、Pydantic 输出校验、mock fallback 和模型输出归一化兜底；输出 `skillGaps`，每项包含 `skill`、`currentLevel`、`targetLevel`、`priority`、`reason`，且至少 4 项。
- 后端新增 Skill Gap Analysis client/service/controller，组装最新能力画像和最新目标拆解结果作为输入，复用 `agent_runs` 持久化输入输出和成功/失败状态；新增 `GET /api/goals/{goalId}/skill-gap` 查询最新成功结果，新增 `POST /api/goals/{goalId}/skill-gap/analyze` 触发重新生成。
- 前端目标详情页新增 `Skill Gap Analysis` 区域，支持生成中、成功、失败、重新生成，并用表格展示技能、当前等级、目标等级、优先级和原因；新增对应 Next.js 同源 API Route。
- 本机验证：Agent `.venv` 下 `pytest` 通过，`9 passed, 1 warning`；Agent `ruff check .` 通过；后端临时 Java 17 `mvn test` 通过，`Tests run: 44, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：`POST /agent/skill-gap/analyze` 可返回结构化 `skillGaps`；`POST /api/goals/3/skill-gap/analyze` 成功返回 `runId`、`goalId` 和技能差距列表；`GET /api/goals/3/skill-gap` 可读取最新成功技能差距分析；数据库 `agent_runs` 已写入 `Skill Gap Analyzer` 的 `SUCCESS` 记录；前端目标详情页 `Skill Gap Analysis` 区域显示 `Ready` 并展示技能差距表格。
- 已知小问题：与 Day 07 类似，`POST /api/goals/{goalId}/skill-gap/analyze` 当次响应中 `createdAt` 可能为 `null`，随后通过 `GET /api/goals/{goalId}/skill-gap` 可读到数据库生成的时间戳；当前前端不依赖该字段。

### Day 09 - Project Recommender - 已完成

- Agent 服务新增 `Project Recommender` JSON-only prompt、`ProjectRecommendRequest/Response` schema、`POST /agent/project/recommend` 接口、Pydantic 输出校验、mock fallback 和模型输出归一化兜底；输出 `recommendedProject`、`reason`、`difficulty`、`durationDays`、`dailyTimeHours`、`coreTechStack`、`finalDeliverables`，MVP 推荐项目固定为 `AI Developer Learning Planner`。
- 后端新增 Project Recommendation client/service/controller，组装最新能力画像、最新目标拆解、最新技能差距、目标周期和每日可用时间作为输入，复用 `agent_runs` 持久化输入输出和成功/失败状态；新增 `GET /api/goals/{goalId}/project-recommendation` 查询最新成功推荐，新增 `POST /api/goals/{goalId}/project-recommendation/recommend` 触发重新生成。
- 前端目标详情页新增 `Project Recommendation` 区域，支持生成中、成功、失败、重新生成，并展示推荐项目、推荐理由、难度、周期、每日时间、核心技术栈和最终交付物；新增对应 Next.js 同源 API Route；“Generate Learning Plan” 入口已占位禁用，留给 Day 10 接入。
- 本机验证：Agent `.venv` 下 `pytest` 通过，`13 passed, 1 warning`；Agent `ruff check .` 通过；后端临时 Java 17 `mvn test` 通过，`Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：`POST /agent/project/recommend` 可返回结构化项目推荐；`POST /api/goals/{goalId}/project-recommendation/recommend` 成功返回 `runId`、`goalId` 和推荐项目详情；`GET /api/goals/{goalId}/project-recommendation` 可读取最新成功项目推荐；数据库 `agent_runs` 已写入 `Project Recommender` 的 `SUCCESS` 记录；前端目标详情页 `Project Recommendation` 区域显示 `Ready` 并展示项目推荐卡片。

### Day 10 - Plan Generator - 已完成

- Agent 服务新增 `Plan Generator` JSON-only prompt、`PlanGenerateRequest/Response` schema、`POST /agent/plan/generate` 接口、Pydantic 输出校验、mock fallback 和模型输出归一化兜底；输出 `planTitle`、`durationDays`、`days`，每天包含 `dayIndex`、`theme`、`tasks`，每个任务包含 `title`、`description`、`estimatedMinutes`、`type`、`deliverable`、`priority`。
- 后端新增 `learning_plans` 和 `daily_tasks` migration、Entity、Repository、Plan Generator client/service/controller；新增 `POST /api/plans/generate`，请求体为 `{ "goalId": number }`；新增 `GET /api/plans/{planId}` 查询计划总览。生成计划时会组装最新能力画像、目标拆解、技能差距、项目推荐、目标周期和每日可用时间作为输入，保存 `Plan Generator` 的 `agent_runs`，并事务性保存 learning plan 和 daily tasks。
- 前端目标详情页的 `Generate Learning Plan` 按钮已接入真实生成流程，成功后跳转 `/plans/[planId]`；新增 `POST /api/plans/generate`、`GET /api/plans/[planId]` 同源 API Route；新增 `/plans/[planId]` 计划总览页，按 Day 展示主题、任务数、预计时长、任务类型、优先级和交付物；补充 `/plans` 计划列表页和 `GET /api/plans` 同源 API Route，可查看所有已生成计划并进入详情；`/goals` 和 `/plans` 列表每条记录均支持暂停/激活和删除。
- 本机验证：Agent `.venv` 下 `pytest` 通过，`17 passed, 1 warning`；Agent `ruff check .` 通过；后端临时 Java 17 `mvn test` 通过，`Tests run: 66, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：`POST /agent/plan/generate` 返回完整 14/21 天计划；`POST /api/plans/generate` 成功写入 `agent_runs`、`learning_plans`、`daily_tasks`；`GET /api/plans/{planId}` 可读取计划详情；前端从目标详情页生成计划后可跳转并展示 `/plans/[planId]`；公网 `/plans` 可查看计划列表；`/goals` 和 `/plans` 列表中的暂停/激活、删除操作可用。

### Day 11 - 每日任务页面 - 已完成

- 后端新增每日任务 API：`GET /api/plans/{planId}/tasks` 查询计划全部任务，`GET /api/plans/{planId}/tasks/today?dayIndex={dayIndex}` 查询指定日任务，`PUT /api/plans/{planId}/tasks/{taskId}/status` 更新任务状态并校验任务归属当前计划。
- 任务状态统一为 `PENDING`、`IN_PROGRESS`、`DONE`、`SKIPPED`；新增 `V5__rename_completed_daily_task_status.sql` 将旧 `COMPLETED` 数据迁移为 `DONE`。
- 前端新增 `/plans/[planId]/today` 每日任务页面，展示任务标题、描述、预计时间、类型、交付物、优先级和状态；支持上一天/下一天切换，并可直接切换任务状态。新增 `/tasks/today` 入口，自动打开最新可用计划；`/plans` 和 `/plans/[planId]` 增加每日任务入口。
- 本机验证：后端临时 Java 17 `mvn test` 通过，`Tests run: 74, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：Flyway `V5` 执行正常；`GET /api/plans/{planId}/tasks/today?dayIndex=1` 可返回任务；`PUT /api/plans/{planId}/tasks/{taskId}/status` 可持久化状态更新；公网 `/plans/{planId}/today` 可展示任务并在刷新后保留任务状态。

### Day 12 - 进度提交模块 - 已完成

- 后端新增 `progress_logs` migration、Entity、Repository、Service 和 Controller；新增 `POST /api/progress` 保存计划日进度，接收 `planId`、`dayIndex`、`userFeedback`、`completedTaskIds`、`unfinishedTaskIds` 和 `blockers`，并校验任务归属当前计划当天。
- 进度提交时会同步每日任务状态：`completedTaskIds` 对应任务置为 `DONE`，`unfinishedTaskIds` 对应任务置为 `PENDING`；`review_result_json` 预留给 Day 13 Progress Reviewer。
- 后端新增 `GET /api/progress/{planId}?dayIndex={dayIndex}` 查询进度历史，按创建时间倒序返回，可用于页面刷新后回看最近一次提交。
- 前端新增 `/api/progress`、`/api/progress/[planId]` 同源 API Route；`/plans/[planId]/today` 增加进度提交区域，支持勾选完成/未完成任务、填写自由反馈和阻塞项，并展示最近一次提交结果。
- 本机验证：后端临时 Java 17 `mvn test` 通过，`Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：Flyway `V6` 执行正常；`POST /api/progress` 可写入 `progress_logs` 并同步任务状态；`GET /api/progress/{planId}?dayIndex=1` 可返回历史记录；公网 `/plans/{planId}/today` 可提交进度，刷新后保留并展示最近一次提交。

### Day 13 - Progress Reviewer - 已完成

- Agent 服务新增 `Progress Reviewer` JSON-only prompt、`ProgressReviewRequest/Response` schema、`POST /agent/progress/review` 接口、Pydantic 输出校验、mock fallback 和模型输出归一化兜底；输出 `completedTasks`、`unfinishedTasks`、`blockers`、`impact` 和 `suggestion`，其中 `impact` 统一为 `none`、`minor`、`medium`、`major`。
- 后端新增 Progress Reviewer client/request/response，进度提交时组装 `dayIndex`、`todayTasks`、`userFeedback`、完成/未完成任务和阻塞项作为输入；调用成功后保存 `Progress Reviewer` 的 `agent_runs` 成功记录，并将复盘结果写入 `progress_logs.review_result_json`；调用失败时保存失败 `agent_runs` 并按现有 Agent 502 策略透出错误。
- 前端 `/plans/[planId]/today` 的最近提交记录新增复盘展示，包含 impact 标签、suggestion 和 reviewer blockers；新增 `ProgressReviewResult`、impact label/class helper。
- 本机验证：Agent `.venv` 下 `pytest` 通过，`21 passed, 1 warning`；Agent `ruff check .` 通过；后端临时 Java 17 `mvn test` 通过，`Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：`POST /agent/progress/review` 可返回结构化复盘；提交 `POST /api/progress` 后 `progress_logs.review_result_json` 包含 impact/suggestion；数据库 `agent_runs` 已写入 `Progress Reviewer` 的 `SUCCESS` 记录；公网 `/plans/{planId}/today` 刷新后可展示最近一次复盘。

### Day 14 - Plan Adjuster - 已完成

- Agent 服务新增 `Plan Adjuster` JSON-only prompt、`PlanAdjustRequest/Response` schema、`POST /agent/plan/adjust` 接口、Pydantic 输出校验、mock fallback 和模型输出归一化兜底；输入包含 `currentPlan`、`todayTasks`、`progressReview`、`unfinishedTasks` 和 `nextDayTasks`，输出包含 `nextDayTasks`、`movedTasks`、`splitTasks` 和 `reason`。
- 后端新增 Plan Adjuster client/request/response，`POST /api/progress` 在 Progress Reviewer 成功后调用 Plan Adjuster；未完成任务会以 `Carry over: ...` 新任务追加到后续日期，过大任务会拆分为多个后续任务，原当天未完成任务保留并置为 `SKIPPED`，避免刷新当天页面时任务消失。
- 调整结果会写入 `progress_logs.review_result_json.planAdjustment`，并追加到 `learning_plans.plan_json.adjustmentHistory`；本次调用写入 `agent_runs`，`agent_name = Plan Adjuster`。
- 前端 `/plans/[planId]/today` 的最近提交记录新增 Tomorrow adjustment 展示，包含调整原因、moved tasks 和 split tasks。
- 本机验证：Agent `.venv` 下 `pytest` 通过，`23 passed, 1 warning`；Agent `ruff check .` 通过；后端临时 Java 17 `mvn test` 通过，`Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：提交含未完成任务的 `POST /api/progress` 后，`agent_runs` 已写入 `Plan Adjuster` 的 `SUCCESS` 记录；`progress_logs.review_result_json->'planAdjustment'` 包含 `reason`、`movedTasks` 和 `nextDayTasks`；`learning_plans.plan_json->'adjustmentHistory'` 已追加调整历史；`daily_tasks` 中 Day 1 未完成任务置为 `SKIPPED`，Day 2 新增 `Carry over: ...` 任务且状态为 `PENDING`；公网 `/plans/{planId}/today?dayIndex=2` 可展示新增 carry-over 任务。

### Day 15 - 跑通完整闭环 - 已完成

- 前端目标详情页新增 `Demo Chain` 侧栏组件和 `Run to Plan` 按钮，会按顺序补齐缺失的能力画像、目标拆解、技能差距分析和项目推荐，并调用 `POST /api/plans/generate` 生成计划后跳转 `/plans/[planId]`。
- 首页从早期 Day 08 文案更新为 Day 15 demo flow，明确当前可演示目标创建、Agent 链路、计划生成、每日进度提交和计划调整。
- README 新增“当前 Demo 路径”，包含推荐 demo 输入、前端操作路径和数据库核查点，便于服务器复现完整闭环。
- 本机验证：前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：按 README demo 路径创建目标、点击 `Run to Plan`、提交 Day 1 进度并检查 Day 2 carry-over 或 split 任务，完整闭环通过。

### Day 16 - 异步任务 - 已完成

- 后端新增 `async_jobs` migration、Entity、Repository、Service、Runner 和 Controller；新增 `POST /api/jobs/plan-generation`、`POST /api/jobs/progress-submission` 和 `GET /api/jobs/{jobId}`，job 状态为 `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`。
- 异步计划生成后台复用 `LearningPlanService.generatePlan`，完成后 job result 包含 `LearningPlanResponse`；异步进度提交后台复用 `ProgressLogService.submitProgress`，完成后仍会保存 Progress Reviewer 复盘、Plan Adjuster 调整、`agent_runs` 和 `progress_logs`。
- 前端新增 job 代理路由和轮询 helper；目标详情页 `Run to Plan`、项目推荐卡片 `Generate Learning Plan`、每日任务页进度提交都改为发起 job 并轮询状态，成功后跳转或刷新，失败时展示错误信息。
- 本机验证：后端临时指定 `JAVA_HOME=C:\Program Files\Java\jdk-17` 后 `mvn test` 通过，`Tests run: 85, Failures: 0, Errors: 0, Skipped: 0`；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：执行 Flyway `V7` 后，已跑通 `Run to Plan` 和 Day 1 进度提交；异步计划生成和异步进度提交测试通过，`async_jobs` 可记录 `PLAN_GENERATION`、`PROGRESS_SUBMISSION` 的成功状态，且计划、任务、复盘和计划调整仍正确落表。

### Day 17 - 安全与校验 - 已完成

- 后端新增 `auth` 模块：`POST /api/auth/register`、`POST /api/auth/login`、BCrypt 密码 hash、HMAC JWT 签发和 `Authorization: Bearer ...` 解析。
- Spring Security 默认保护除 `/api/health`、`/api/auth/**` 和 OpenAPI docs 外的后端接口；goals、plans、tasks、progress、jobs 以及 goal 下的 Agent 结果接口均按当前 JWT 用户限制访问，越权资源按 404 处理。
- 新增 `V8__add_async_job_user_id.sql`，`async_jobs.user_id` 记录 job 创建者；创建 job 时先校验 goal/plan 归属，查询 job 时只允许当前用户读取自己的 job。
- 前端新增 `/login` 页面和 `/api/auth/login`、`/api/auth/register`、`/api/auth/logout` 同源 route；登录或注册成功后写入 `ai_planner_token` cookie，服务端页面 fetch 和所有同源代理自动转发 Bearer token。
- Agent 服务新增 `model_retry.py`，真实模型调用在 HTTP、JSON 解析或 Pydantic 校验失败时最多尝试 3 次；mock fallback 策略保持不变。
- 本机验证：后端临时指定 `JAVA_HOME=C:\Program Files\Java\jdk-17` 后 `mvn test` 通过，`Tests run: 85, Failures: 0, Errors: 0, Skipped: 0`；Agent `.venv` 下 `pytest` 通过，`25 passed, 1 warning`，`ruff check .` 通过；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 通过。
- 服务器验收：执行 Flyway `V8` 后 `async_jobs.user_id` 和 `idx_async_jobs_user_id` 已可用；未登录访问受保护接口返回 `401`；登录后 goals/plans 页面请求可携带 token，顶部导航显示登录用户名；使用不同用户访问他人 goal/plan/job 按 404 处理；完整 demo 链路仍可从登录、创建目标、`Run to Plan`、提交 Day 1 进度到 Day 2 carry-over/split 任务闭环运行。
- 验收修复：`SecurityConfig` 曾因 `.exceptionHandling(...)` 后缺少分号导致服务器后端编译失败，已补齐并重新通过后端测试；前端生产构建在 HTTP 服务器上曾把 auth cookie 设置为 `Secure`，浏览器因此不保存 token，已改为按实际站点协议或 `AUTH_COOKIE_SECURE` 显式配置决定是否加 `Secure`，并新增 `ai_planner_username` cookie 用于导航展示。

### Day 18 - 日志与可观测性 - 已完成

- 后端新增 `V9__add_agent_run_observability_fields.sql`，为 `agent_runs` 增加 `plan_id`、`request_id`、`idx_agent_runs_plan_id`、`idx_agent_runs_request_id` 和 `idx_agent_runs_created_at`。
- 后端新增 Agent Runs 查询能力：`GET /api/agent-runs` 支持按 `goalId`、`planId`、`agentName` 过滤，`GET /api/agent-runs/{runId}` 返回 input/output JSON、错误原因、latency、status、requestId 和关联 goal/plan；登录后只允许读取当前用户自己的记录，越权按 404 处理。
- 后端新增请求日志过滤器和 `ObservabilityContext`，每次请求会生成或透传 `X-Request-Id`，响应也回写同名 header；日志 pattern 包含 MDC requestId；Agent run 落库时输出结构化日志，包含 `runId`、`agentName`、`status`、`goalId`、`planId`、`latencyMs`、`requestId` 和失败错误。
- 所有 Spring 到 Agent 服务的 HTTP client 都会透传 `X-Request-Id`；异步 job 会把创建请求的 requestId 显式带入后台线程，避免 `@Async` 丢失日志上下文。
- Agent 服务新增 request logging middleware，会接收或生成 `X-Request-Id`，响应回写 header，并记录 method、path、status、latencyMs、requestId；`retry_model_call` 的失败尝试日志也包含 requestId，并新增错误分类：网络异常、超时、429、5xx、JSON/字段/Pydantic 校验失败视为可重试；401、403、404、400 等配置、权限或请求错误视为不可重试。
- 服务器验收时发现 Goal Decomposer 真实模型输出偶发不符合早期 schema，导致 `Goal decomposer model response was invalid.` 和 502；已补齐 Goal Decomposer 模型输出归一化，支持 `subGoals`、`sub_goals`、`goals`、`steps`、`milestones` 等外壳，支持中英文字段别名和优先级归一化。可重试输出问题重试耗尽后会 fallback，API key/权限/请求错误不会 fallback。
- 前端新增 `/agent-runs` 列表页、`/agent-runs/[runId]` 详情页、顶部导航 Agent Runs 入口，以及 `/api/agent-runs`、`/api/agent-runs/[runId]` 同源代理；所有同源 API proxy 会转发或生成 `X-Request-Id`。
- 本机验证：后端临时指定 `JAVA_HOME=C:\Program Files\Java\jdk-17` 后 `mvn test` 通过，`Tests run: 89, Failures: 0, Errors: 0, Skipped: 0`；Agent `.venv` 下 `pytest` 通过，`28 passed, 1 warning`，`ruff check .` 通过；前端 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 均通过。
- 服务器验收：执行 Flyway `V9` 后登录并跑完整 `Run to Plan`，`/agent-runs` 可查看 Agent 历史，详情页可查看 input/output JSON；数据库中 `agent_runs.request_id` 已写入，Plan Generator/Progress Reviewer/Plan Adjuster 可关联 `plan_id`，可通过同一 requestId 关联后端与 Agent 服务日志。

## 当前注意事项

- `Plan/` 和 `PROJECT_PLAN.md` 是本地计划材料，已被 `.gitignore` 排除，不要上传。
- 服务器使用本机 PostgreSQL 14，监听 `localhost:5432`，数据库 `ai_planner`，用户 `ai_planner`。
- 本机 `psql -U ai_planner` 可能触发 peer authentication；服务器查询建议使用 `-h localhost` 和 `PGPASSWORD=ai_planner_dev_password`。
- 当前服务器 Docker Hub 直连解析异常；如需 Docker 拉取 PostgreSQL，可临时使用 `docker.m.daocloud.io/library/postgres:16`，但注意本机 PostgreSQL 已占用宿主机 `5432`。
- 当前服务器 Python 为 `3.10.12`，Agent 服务按 Python `>=3.10` 配置。
- Next.js `16.2.7` 要求 Node.js `>=20.9.0`；服务器已能启动前端并通过 `curl -I http://localhost:3000`。
- 公网验收建议使用 `npm run build && npm run start:server`，避免 `next dev` 的 `/_next/webpack-hmr` WebSocket 开发态报错。
- 服务器测试或重启服务前，应先在项目根目录执行 `set -a && source .env && set +a`，确保 `DEEPSEEK_API_KEY`、`AGENT_SERVICE_BASE_URL`、数据库连接等环境变量进入当前 shell；已运行进程不会自动读取新环境变量，导入后需要重启对应服务。
- 如果 Agent 服务改用 `8001` 验收，后端启动时需要设置 `AGENT_SERVICE_BASE_URL=http://localhost:8001`。
- Day 06 曾出现后端调用 Agent 返回 422 的问题。原因是 Spring `RestClient` 默认请求工厂和 Uvicorn/FastAPI 之间存在请求解析/upgrade 兼容问题。修复点在 `HttpProfileAnalyzerClient`: 显式 `Content-Type: application/json`、`Accept: application/json`，并使用 `SimpleClientHttpRequestFactory()`；同时透出 Agent 返回体方便排错。
- Day 07 的 `HttpGoalDecomposerClient` 沿用 Day 06 的请求工厂、`Content-Type`、`Accept` 和错误体透出策略，避免同类 FastAPI/Uvicorn 解析问题。
- Day 08 的 `HttpSkillGapAnalyzerClient` 沿用 Day 06/07 的请求工厂、`Content-Type`、`Accept` 和错误体透出策略，避免同类 FastAPI/Uvicorn 解析问题。
- Day 09 的 `HttpProjectRecommenderClient` 沿用 Day 06/07/08 的请求工厂、`Content-Type`、`Accept` 和错误体透出策略，避免同类 FastAPI/Uvicorn 解析问题。
- Day 10 的 `HttpPlanGeneratorClient` 沿用 Day 06/07/08/09 的请求工厂、`Content-Type`、`Accept` 和错误体透出策略，避免同类 FastAPI/Uvicorn 解析问题。
- Day 08 服务器验收时发现：真实 DeepSeek 输出偶尔会返回字段别名、中文优先级、嵌套结构或少于 4 条技能差距，导致前端重新生成时概率性 502。已在 `skill_gap_analyzer.py` 增加归一化和兜底补齐；如果模型调用失败或输出完全不可用，会降级到 mock fallback，避免 UI 随机失败。
- Day 09 项目推荐 Agent 也增加了模型输出归一化和 mock fallback；MVP 阶段后端会将推荐项目名统一归一为 `AI Developer Learning Planner`，避免模型推荐偏离当前项目主线。
- Day 10 计划生成 Agent 也增加了模型输出归一化和 mock fallback；如果模型少返回天数或任务字段漂移，Agent 和后端都会补齐到目标周期，确保 `learning_plans` 与 `daily_tasks` 可用于 Day 11。
- Day 11 将每日任务完成态统一命名为 `DONE`，不再使用早期前端类型中的 `COMPLETED`；如服务器已有旧值，`V5` 会迁移。
- Day 13 的 `HttpProgressReviewerClient` 沿用 Day 06/07/08/09/10 的请求工厂、`Content-Type`、`Accept` 和错误体透出策略，避免同类 FastAPI/Uvicorn 解析问题。
- Day 13 的 `progress_logs.review_result_json` 当前保存 Progress Reviewer 的结构化输出；Day 14 起同一 JSON 中的 `planAdjustment` 保存 Plan Adjuster 的局部调整结果。
- Day 14 的 `HttpPlanAdjusterClient` 沿用 Day 06/07/08/09/10/13 的请求工厂、`Content-Type`、`Accept` 和错误体透出策略，避免同类 FastAPI/Uvicorn 解析问题。
- Day 14 为避免提交进度后当天页面任务消失，未完成任务不会从当天硬移除，而是将原任务置为 `SKIPPED` 并在后续日期新增 carry-over 或 split 任务；验收时应查看下一天任务是否新增，而不是要求原任务从当天记录中删除。
- Day 15 的 `Run to Plan` 会跳过目标详情页已存在的 Agent 结果，只补齐缺失步骤；最后仍会新建一个 learning plan，适合 demo 验收但可能产生多个测试计划。
- Day 16 当前采用 Spring `@Async` + `async_jobs` 数据库表实现 MVP 异步任务，保留原同步 `POST /api/plans/generate` 和 `POST /api/progress` 供兼容；前端 demo 路径使用新的 job API。暂未引入 Celery/Redis worker，后续如需要跨进程队列、重试、超时取消或任务恢复，再补 Redis/Celery。
- `agent_runs` 中 Day 06 前几条 `FAILED` 是修复前的排错记录，可以保留作为审计记录；后续 `SUCCESS` 为正常调用。
- Day 18 后 Agent 服务真实模型调用不再“所有异常直接 fallback”：`ModelCallRetryExhaustedError` 表示可重试错误已耗尽，此时按各 Agent 策略 fallback；`ModelCallNonRetryableError` 表示 401/403/404/400 等配置、权限或请求错误，应直接暴露给前端和 `agent_runs.error_message`，避免掩盖密钥或模型配置问题。
- Day 18 服务器验收时暴露过 Goal Decomposer 偶发失败，原因是真实模型输出字段别名或外壳不稳定。已在 `goal_decomposer.py` 增加归一化和 fallback；如果后续其它 Agent 出现同类 `model response was invalid`，优先按“归一化真实输出 + 错误分类重试”处理，不要直接无条件 mock。
- 开发机默认 `JAVA_HOME` 可能指向 Java 8；后端需要 Java 17。项目运行和测试以服务器 Java 环境为准。

## 后续每日更新规则

每天任务完成后，更新本文档：

1. 修改“当前阶段”，写明已完成 Day、当前主题和下一阶段。
2. 在“进度摘要”追加当天完成内容、验证结果和遗留事项，保持简洁。
3. 如新增服务、端口、环境变量、目录或启动方式，同步更新相关章节。
4. 保持 `README.md` 面向项目使用者，`AGENTS.md` 面向后续 agent 交接。
