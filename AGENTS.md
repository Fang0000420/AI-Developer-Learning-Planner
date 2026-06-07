# AGENTS.md

面向后续 coding agent 的项目交接与阶段记录。每天任务完成后，持续更新本文档，让新的上下文可以快速接上项目状态。

## 当前阶段

- 当前日期: 2026-06-07
- 当前进度: Day 02 进行中 - 任务 2 已完成
- 当前主题: Spring Boot 后端初始化
- 下一阶段: Day 02 - 任务 3 创建首批数据库表

Day 01 的目标是把项目从想法落成可持续开发的 monorepo 结构，明确服务边界、端口、启动方式和第一版文档。目前基础骨架已经完成，并已上传到 GitHub；服务器也已经拉取代码并完成 Docker Compose 配置解析验证。

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

## 当前注意事项

- `Plan/` 和 `PROJECT_PLAN.md` 是本地计划材料，已被 `.gitignore` 排除，不要上传。
- `.env.example` 可以提交；真实 `.env` 和任何密钥不能提交。
- 所有任务默认在服务器 `/home/AI-Developer-Learning-Planner` 中执行。
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
- 基础验收: monorepo 结构、README、Docker Compose、`.env.example`、架构文档均已创建。
- GitHub 验收: 项目已上传，`Plan/` 和 `PROJECT_PLAN.md` 未上传。
- 服务器验收: 代码已拉取，`git status` 干净，Docker Compose 配置解析通过。
- 遗留事项: 进入 Day 02，初始化 Spring Boot 后端服务。

### Day 02 - Spring Boot 后端初始化 - 进行中

- 完成时间: 2026-06-07
- 已完成任务: 任务 1 创建 Spring Boot 项目；任务 2 配置服务器开发环境。
- 代码验收: `backend/` 已具备 Maven 项目结构、`pom.xml`、Spring Boot 启动类、`application.yml` 和测试骨架。
- 服务器验收: 用户在服务器执行 `mvn test` 通过，结果 `Tests run: 1, Failures: 0, Errors: 0`，`BUILD SUCCESS`。
- 任务 2 完成内容: 已配置 PostgreSQL datasource、JPA、Flyway、服务端口 `8080`，并新增 `GET /api/health`。
- 验证说明: 按用户要求，不在开发机做 Maven 依赖下载和测试运行，后续在服务器拉取后执行 `mvn test` 与 `mvn spring-boot:run`。
- 遗留事项: 任务 3 创建 `users`、`goals` 首批表；任务 4 做后端冒烟验证。
