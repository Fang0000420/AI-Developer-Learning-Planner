# AGENTS.md

面向后续 coding agent 的项目交接与阶段记录。本文档只保留当前状态、关键约定、已完成阶段摘要和仍需注意的坑点；每日流水细节可回看 `Plan/` 或 Git 历史。

## 当前阶段

- 当前日期: 2026-06-09
- 当前进度: Day 09 已完成
- 当前主题: Project Recommender
- 下一阶段: Day 10 - Plan Generator
- 当前模型策略: 能力画像、目标拆解、技能差距分析和项目推荐优先使用 DeepSeek，目标模型暂定 `deepseek-v4-pro`；未配置 `DEEPSEEK_API_KEY` 时 Agent 服务使用 mock fallback；服务器暂不部署 Clash 或其他代理方案。

Day 09 已完成并在服务器验收通过：前端目标详情页可以触发项目推荐，后端调用 Agent 服务，Agent 服务调用 DeepSeek 并对模型输出做归一化兜底，后端将项目推荐结果保存到 `agent_runs.output_json`，前端可读取并展示 `recommendedProject`、`reason`、`difficulty`、`durationDays`、`dailyTimeHours`、`coreTechStack`、`finalDeliverables`。

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

### Agent Service

- `GET /health`
- `POST /agent/profile/analyze`
- `POST /agent/goal/decompose`
- `POST /agent/skill-gap/analyze`
- `POST /agent/project/recommend`

### Frontend 同源 API Route

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

前端通过 Next.js 同源 API Route 请求后端，避免公网浏览器直接访问服务器自身 `localhost:8080`。

## 数据库当前状态

已落地 Flyway migrations：

- `V1__create_users_and_goals.sql`: `users`, `goals`
- `V2__create_skill_profiles_and_agent_runs.sql`: `skill_profiles`, `agent_runs`

MVP 当前未接入登录；创建目标时 `userId` 可选，未传时自动使用或创建 `demo-user`。

Day 07 目标拆解、Day 08 技能差距分析和 Day 09 项目推荐暂不新增专表；最新结果从 `agent_runs` 中按 `goal_id`、对应 `agent_name` 和 `status = 'SUCCESS'` 查询，并解析 `output_json` 返回前端。当前使用的 `agent_name` 包括 `Goal Decomposer`、`Skill Gap Analyzer` 和 `Project Recommender`。

## 环境变量重点

- `NEXT_PUBLIC_BACKEND_API_BASE_URL=http://localhost:8080`
- `AGENT_SERVICE_BASE_URL=http://localhost:8000`
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
- Day 08 服务器验收时发现：真实 DeepSeek 输出偶尔会返回字段别名、中文优先级、嵌套结构或少于 4 条技能差距，导致前端重新生成时概率性 502。已在 `skill_gap_analyzer.py` 增加归一化和兜底补齐；如果模型调用失败或输出完全不可用，会降级到 mock fallback，避免 UI 随机失败。
- Day 09 项目推荐 Agent 也增加了模型输出归一化和 mock fallback；MVP 阶段后端会将推荐项目名统一归一为 `AI Developer Learning Planner`，避免模型推荐偏离当前项目主线。
- `agent_runs` 中 Day 06 前几条 `FAILED` 是修复前的排错记录，可以保留作为审计记录；后续 `SUCCESS` 为正常调用。
- 开发机默认 `JAVA_HOME` 可能指向 Java 8；后端需要 Java 17。项目运行和测试以服务器 Java 环境为准。

## 后续每日更新规则

每天任务完成后，更新本文档：

1. 修改“当前阶段”，写明已完成 Day、当前主题和下一阶段。
2. 在“进度摘要”追加当天完成内容、验证结果和遗留事项，保持简洁。
3. 如新增服务、端口、环境变量、目录或启动方式，同步更新相关章节。
4. 保持 `README.md` 面向项目使用者，`AGENTS.md` 面向后续 agent 交接。
