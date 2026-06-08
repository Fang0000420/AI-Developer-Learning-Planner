# AGENTS.md

面向后续 coding agent 的项目交接与阶段记录。每天任务完成后，持续更新本文档，让新的上下文可以快速接上项目状态。

## 当前阶段

- 当前日期: 2026-06-08
- 当前进度: Day 04 进行中，任务 1、2 服务器验收完成，任务 3 本机完成
- 当前主题: Next.js 前端初始化
- 下一阶段: Day 04 任务 4 - 调用后端 health API

Day 04 任务 1 已完成 `frontend/` Next.js 工程初始化，并已在服务器 `http://localhost:3000` 通过 `curl -I` 验收，返回 `HTTP/1.1 200 OK`。任务 2 基础工作台 UI 已可从服务器公网 `http://47.99.120.38:3000` 访问。任务 3 已完成 `/goals/new` 目标输入页面骨架，本机已验证表单展示、Zod 校验和有效草稿提交。

## 运行环境约定

所有每日任务的开发、启动、测试和验收默认都在服务器 `/home/AI-Developer-Learning-Planner` 中执行。本机只用于计划文件编辑、必要的 Git 辅助操作和与 agent 协作，不作为项目服务运行环境。

后续更新 `Plan/` 中的每日任务时，应继续保持服务器运行口径，验收标准也应以服务器执行结果为准。

## 项目定位

AI Developer Learning Planner 是一个面向程序员、研究生和技术转型者的 AI 学习与技术成长规划 Agent 系统。用户输入技术背景、当前技能、学习目标、求职目标、每天可投入时间和计划周期后，系统会生成能力画像、拆解目标、识别技能差距、推荐项目方向、生成每日任务，并根据每日进度动态调整后续计划。

MVP 优先跑通以下闭环:

1. 用户输入背景、技能、目标和计划周期。
2. Agent 服务生成能力画像和目标拆解。
3. 系统识别技能差距并推荐项目方向。
4. 系统生成 14 天或 21 天每日任务计划。
5. 用户每天提交完成情况。
6. Agent 服务复盘进度并调整后续任务。

## 当前仓库结构

```text
.
├── AGENTS.md
├── README.md
├── .env.example
├── frontend/
│   ├── package.json
│   └── src/
├── backend/
│   ├── pom.xml
│   └── src/
├── agent-service/
├── infra/
│   └── docker-compose.yml
├── docs/
│   └── architecture.md
└── .github/workflows/
```

## 服务与端口约定

| 服务 | 目录 | 默认地址 | 端口 |
| --- | --- | --- | --- |
| Frontend | `frontend/` | `http://localhost:3000` | `3000` |
| Backend | `backend/` | `http://localhost:8080` | `8080` |
| Agent Service | `agent-service/` | `http://localhost:8000` | `8000` |
| PostgreSQL | `infra/` | `localhost:5432` | `5432` |
| Redis | `infra/` | `localhost:6379` | `6379` |

环境变量命名以 `.env.example` 为准，架构说明以 `docs/architecture.md` 为准。

## Day 01 已完成内容

- 初始化 Git 仓库并使用 `main` 分支。
- 创建 monorepo 目录: `frontend/`, `backend/`, `agent-service/`, `infra/`, `docs/`, `.github/workflows/`。
- 为初始目录添加占位 README，保证目录结构可被 Git 跟踪。
- 编写 `README.md` 初稿，覆盖项目定位、MVP 闭环、技术栈、服务拆分、目录结构和服务器启动占位步骤。
- 创建 `infra/docker-compose.yml`，配置 PostgreSQL 和 Redis、端口映射、命名 volume、healthcheck、网络和后续服务 TODO。
- 创建 `.env.example`，固定前端、后端、Agent 服务、PostgreSQL、Redis 和 `OPENAI_API_KEY` 的变量命名。
- 创建 `docs/architecture.md`，记录端口约定、服务边界、环境变量说明、MVP 调用流程和 Day 01 工程决策。
- 配置 `.gitignore`，排除 `Plan/` 和 `PROJECT_PLAN.md`。
- 项目已上传到 GitHub，并已在服务器 `/home/AI-Developer-Learning-Planner` 拉取。
- 服务器执行 `docker compose -f infra/docker-compose.yml config` 验证通过。

## Day 02 已完成内容

- 任务 1 创建 Spring Boot 项目已完成。
- `backend/` 已初始化为 Maven + Spring Boot 工程。
- 后端使用 Java 17、Spring Boot 3.4.2，包名为 `com.aidevplanner.backend`。
- 已加入依赖: Spring Web、Spring Data JPA、Validation、PostgreSQL Driver、Flyway、Lombok、Springdoc OpenAPI、Spring Boot Test。
- 已创建 Spring Boot 启动类和轻量测试，避免在数据库配置完成前强制启动应用上下文。
- 任务 1 已在服务器执行 `cd backend && mvn test`，结果 `BUILD SUCCESS`。
- 任务 2 配置服务器开发环境已完成。
- `application.yml` 已配置服务端口 `BACKEND_PORT` 默认 `8080`、PostgreSQL datasource、JPA `ddl-auto=validate`、Flyway 默认 migration 路径。
- 已新增自定义 health 接口 `GET /api/health`，返回后端服务状态。
- 已新增 health 接口 MVC 测试。
- 任务 2 已在服务器验证通过，`GET /api/health` 返回 `{"status":"UP","service":"ai-developer-learning-planner-backend"}`。
- 任务 3 创建首批数据库表已完成。
- 已新增 Flyway migration `V1__create_users_and_goals.sql`，创建 `users` 和 `goals` 表，并为 `goals.user_id`、`goals.status` 添加索引。
- 已新增 `User`、`Goal`、`GoalStatus` JPA 类型，以及 `UserRepository`、`GoalRepository`。
- 已新增不依赖数据库的实体单元测试，覆盖用户字段和目标默认状态。
- 任务 3 已在服务器验证通过，`flyway_schema_history` 中 version `1`、description `create users and goals`、success `t`。
- 任务 4 后端冒烟验证已完成。
- 服务器 `GET /api/health` 返回 `{"status":"UP","service":"ai-developer-learning-planner-backend"}`。
- 服务器数据库检查确认 `flyway_schema_history`、`goals`、`users` 三张表存在。

## Day 03 已完成内容

- `agent-service/` 已初始化为 Python + FastAPI 工程，包含 `app/main.py`、`app/api/`、`app/schemas/`、`app/services/`、`tests/` 和 `pyproject.toml`。
- Agent 服务依赖已固定版本: FastAPI `0.136.3`、Pydantic `2.13.4`、Uvicorn `0.49.0`、HTTPX `0.28.1`、Pytest `9.0.3`、Ruff `0.15.16`。
- `pyproject.toml` 已声明 Python `>=3.10`，匹配服务器 Python `3.10.12`；Ruff target 为 `py310`。
- 已实现 `GET /health`，返回服务名、状态和版本号。
- 已定义 `ProfileAnalyzeRequest` 和 `ProfileAnalyzeResponse`，并实现 `POST /agent/profile/analyze` 固定结构化假数据接口。
- 已新增基础测试 `tests/test_health.py` 和 `tests/test_profile.py`。
- 本地验证 `pytest` 结果 `2 passed, 1 warning`，`ruff check .` 通过；warning 来自 FastAPI/Starlette TestClient 上游 deprecation。
- 服务器使用 `8001` 临时端口验证通过:
  - `curl http://localhost:8001/health` 返回 `{"service":"ai-developer-learning-planner-agent-service","status":"UP","version":"0.1.0"}`。
  - `POST /agent/profile/analyze` 返回 `currentSkills`、`strengths`、`weaknesses`、`recommendedDirection` 四个字段。

## Day 04 进行中内容

- 任务 1 创建 Next.js 项目已完成。
- `frontend/` 已初始化为 Next.js + React + TypeScript 工程，使用 App Router 和 `src/` 目录结构。
- 前端依赖版本: Next.js `16.2.7`、React `19.2.4`、React DOM `19.2.4`、Tailwind CSS `4.3.0`、ESLint `9`、Prettier `3.8.3`。
- Next.js `16.2.7` 要求 Node.js `>=20.9.0`；后续服务器验收前需确认服务器 Node 版本。
- 已配置 Tailwind CSS 4、PostCSS、ESLint flat config、TypeScript strict、Prettier、`format` 与 `format:check` 脚本。
- 已移除脚手架默认营销页面和 Google 字体构建期依赖，创建项目化基础首页。
- `frontend/README.md` 已补充服务器启动说明和前端脚本说明。
- 本机验证通过: `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build`。
- 本机浏览器验证通过: `http://127.0.0.1:3000` 页面标题为 `AI Developer Learning Planner`，无浏览器控制台 error。
- 服务器验证通过: 服务器执行 `curl -I http://localhost:3000` 返回 `HTTP/1.1 200 OK`。
- 任务 2 建立基础 UI 结构已完成。
- 已创建应用级顶部导航，入口包括 `Dashboard`、`New Goal`、`Plans`、`Today`。
- 首页已改为基础工作台页面，展示当前状态入口: 创建目标、查看计划、今日任务。
- 首页包含 MVP Progress 和 Next Step 区域，第一屏直接进入工作台，不做营销式落地页。
- 已新增 `lucide-react` 用于导航与入口图标。
- 本机验证通过: `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build`。
- 本机浏览器验证通过: 首页可读取 `Dashboard`、`New Goal`、`View Plan`、`Today Tasks`、`MVP Progress`，控制台 error 数为 `0`。
- 任务 2 服务器验证通过: 用户已从浏览器访问服务器公网 `http://47.99.120.38:3000` 并看到工作台页面。
- 任务 3 创建目标输入页面骨架已完成。
- 已新增 `/goals/new` 页面，包含技术背景、学习目标、求职目标、每日可用时间和计划周期字段。
- 已新增 `react-hook-form`、`zod`、`@hookform/resolvers`，实现前端校验骨架。
- 目标表单当前只做前端草稿校验，不提交业务数据、不调用后端。
- 本机验证通过: `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build`。
- 本机浏览器验证通过: `/goals/new` 不再返回 404；空提交显示校验错误；有效数据提交后显示 `Draft passed frontend validation.`；控制台 error 数为 `0`。
- 任务 3 服务器验证待执行: 需在服务器拉取/同步最新代码后重新执行 `npm ci`、构建和 `/goals/new` 页面访问验收。

## 当前注意事项

- `Plan/` 和 `PROJECT_PLAN.md` 是本地计划材料，已被 `.gitignore` 排除，不要上传。
- `.env.example` 可以提交；真实 `.env` 和任何密钥不能提交。
- 所有任务默认在服务器 `/home/AI-Developer-Learning-Planner` 中执行。
- 当前服务器使用本机 PostgreSQL 14，监听 `localhost:5432`，已创建数据库 `ai_planner` 和用户 `ai_planner`。
- 当前服务器 Docker Hub 直连解析异常；如需 Docker 拉取 PostgreSQL，可临时使用 `docker.m.daocloud.io/library/postgres:16`，但注意本机 PostgreSQL 已占用宿主机 `5432`。
- 当前服务器 Python 为 `3.10.12`，Agent 服务已按 Python `>=3.10` 配置。
- Day 04 任务 1 使用 Next.js `16.2.7`，要求 Node.js `>=20.9.0`；服务器已能启动并通过 `curl -I http://localhost:3000` 返回 `HTTP/1.1 200 OK`。
- 服务器公网访问 `next dev` 时，浏览器控制台可能出现 `/_next/webpack-hmr` WebSocket 连接失败。这是开发模式热更新通道问题，不代表页面业务错误；公网验收建议使用 `npm run build && npm run start:server`。
- 当前开发机可启动前端 `http://127.0.0.1:3000`，但项目约定的正式开发、启动、测试和验收仍应以服务器为准。
- 当前服务器 `8000` 端口被占用；Agent 服务默认端口仍为 `8000`，验收时临时使用 `8001`。后续如需长期使用 `8001`，应同步调整 `.env.example`、README 和服务端口约定。
- 服务器已安装 Docker，后续可用以下命令启动基础依赖:

```bash
docker compose -f infra/docker-compose.yml up -d postgres redis
```

- 开发机 Git 可能提示无法读取全局 ignore 文件 `C:\Users\Administrator/.config/git/ignore`，但不影响本仓库 `.gitignore` 生效。
- 开发机默认 `JAVA_HOME` 当前指向 Java 8；后端工程需要 Java 17。项目运行和测试以服务器 Java 环境为准。

## 后续每日更新规则

每天任务完成后，更新本文档:

1. 修改“当前阶段”，写明已完成的 Day 和下一阶段。
2. 在“进度记录”追加当天完成内容、验证结果和遗留问题。
3. 如新增服务、端口、环境变量、目录或启动方式，同步更新相关章节。
4. 保持 `README.md` 面向项目使用者，`AGENTS.md` 面向后续 agent 交接。

## 进度记录

### Day 01 - 项目初始化 - 已完成

- 完成时间: 2026-06-07
- 完成摘要: 建立 monorepo 结构、README、`.env.example`、`infra/docker-compose.yml`、`docs/architecture.md` 和 GitHub 仓库。
- 验收摘要: 服务器已拉取代码，Docker Compose 配置解析通过；`Plan/` 和 `PROJECT_PLAN.md` 已被 `.gitignore` 排除。

### Day 02 - Spring Boot 后端初始化 - 已完成

- 完成时间: 2026-06-07
- 完成摘要: `backend/` 已初始化 Maven + Spring Boot 3.4.2 + Java 17 工程，完成 PostgreSQL/JPA/Flyway 配置、`GET /api/health`、`users`/`goals` migration、JPA Entity/Repository 和基础测试。
- 验收摘要: 服务器 `mvn test` 通过；后端 health 接口返回 `UP`；数据库确认 `flyway_schema_history`、`goals`、`users` 存在。
- 遗留事项: Docker Hub 直连存在 DNS/网络异常；Day 02 验证使用服务器本机 PostgreSQL 14 完成。

### Day 03 - FastAPI Agent 服务初始化 - 已完成

- 完成时间: 2026-06-08
- 完成摘要: `agent-service/` 已初始化 FastAPI 工程，完成依赖固定、`GET /health`、`POST /agent/profile/analyze` 假数据接口、Pydantic schema、service 分层和基础 pytest/ruff 配置。
- 验收摘要: 服务器使用 `8001` 临时端口验证 `/health` 和 `/agent/profile/analyze` 均通过；服务器测试通过，`pytest` 和 `ruff check .` 均可执行。
- 遗留事项: 默认 Agent 端口仍为 `8000`，但服务器当前 `8000` 被占用；后续 Day 04 前端对接时需确认实际 Agent 服务端口。

### Day 04 - Next.js 前端初始化 - 进行中

- 完成时间: 2026-06-08
- 已完成任务: 任务 1 创建 Next.js 项目；任务 2 建立基础 UI 结构；任务 3 创建目标输入页面骨架。
- 完成摘要: `frontend/` 已初始化 Next.js 16 + React 19 + TypeScript + Tailwind CSS 4 工程，补充 ESLint、Prettier、`typecheck`、格式化脚本、应用顶部导航、基础工作台首页、`/goals/new` 目标输入页和前端 README 服务器启动说明。
- 验收摘要: 任务 1 服务器 `curl -I http://localhost:3000` 返回 `HTTP/1.1 200 OK`；任务 2 用户已从服务器公网 `http://47.99.120.38:3000` 访问工作台页面；任务 3 本机 `npm run lint`、`npm run typecheck`、`npm run format:check`、`npm run build` 均通过，本机浏览器验证 `/goals/new` 表单展示、校验和草稿提交正常。
- 遗留事项: 任务 3 服务器侧需在同步最新代码后重新执行 `npm ci`、构建与 `/goals/new` 页面访问验收；后续任务 4 调用后端 health API。
