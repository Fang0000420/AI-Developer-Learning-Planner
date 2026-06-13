# AGENTS.md

面向后续 coding agent 的项目状态快照。此文件描述当前仓库中已经存在的代码事实，
不再沿用早期 Day 叙事作为唯一依据。旧版交接文档已归档到
`docs/archive/AGENTS-2026-06-10.md` 和 `docs/archive/AGENTS-2026-06-13.md`；
阶段性拆解仍可参考 `Plan/`，但应以当前代码和测试为准。

## 当前快照

- 快照日期：`2026-06-13`
- 仓库形态：monorepo，包含 `frontend/`、`backend/`、`agent-service/`、`infra/`
  和根目录 `docker-compose.yml`
- 当前主入口：根目录 `docker-compose.yml`
- 当前文档可信度：
  - 根目录 `README.md` 基本反映已跑通的 MVP 闭环
  - `backend/README.md` 与 `agent-service/README.md` 仍停留在早期阶段说明，已经明显过时
- 当前工作区不是干净状态：`frontend/`、`backend/`、`agent-service/`
  均存在大量未提交改动，新任务必须先判断是继续这些改动，还是只做独立修复

## 已落地能力

### 前端

- `frontend/` 基于 Next.js 16、React 19、TypeScript、Tailwind CSS 4。
- 已存在的页面与工作台包括：
  - 登录页：`/login`
  - 目标列表与新建目标：`/goals`、`/goals/new`
  - 目标详情页及子面板：画像分析、目标拆解、技能差距、项目推荐、成长路径
  - 计划列表、计划详情、今日任务页：`/plans`、`/plans/[planId]`、
    `/plans/[planId]/today`
  - 个人画像页：`/profile`
  - 知识库页与文档详情页：`/knowledge`、`/knowledge/[documentId]`
  - Agent Runs 列表与详情：`/agent-runs`
- 前端已实现同源 API Route 代理，目录位于 `frontend/src/app/api/`，覆盖鉴权、
  goals、plans、progress、knowledge、jobs、agent-runs、locale 等。
- 页面语言已支持 `zh`/`en` 切换；`CN/EN` 开关位于 `frontend/src/app/language-switcher.tsx`，
  通过 `/api/locale` 写入 cookie 后刷新页面。

### Java Backend

- `backend/` 基于 Java 17、Spring Boot 3.4.2、Spring Web、Spring Data JPA、
  Spring Security、Redis、Flyway、PostgreSQL。
- 已存在的核心模块：
  - `auth/`：注册、登录、JWT、认证上下文
  - `goal/`：目标 CRUD、目标级知识偏好
  - `profile/`：用户画像、目标画像快照
  - `goaldecomposition/`：目标拆解
  - `skillgap/`：技能差距分析
  - `projectrecommendation/`：项目推荐
  - `path/`：成长路径分析与推荐
  - `learningplan/`：学习计划、每日任务、计划版本、版本回滚
  - `progress/`：进度提交、复盘结果、自适应节奏控制
  - `knowledge/`：知识库文档、检索预览、策略对比、批量更新
  - `asyncjob/`：异步任务创建与轮询
  - `agent/`：Agent Run 记录、详情查询、请求头与响应头日志
- 当前后端接口已经不仅是基础 CRUD。确认存在的接口包括：
  - `POST /api/auth/register`、`POST /api/auth/login`
  - `GET/POST/PUT/DELETE /api/goals`
  - `GET/PATCH /api/goals/{goalId}/knowledge-preference`
  - `POST /api/goals/{goalId}/profile/analyze`
  - `POST /api/goals/{goalId}/decomposition/decompose`
  - `POST /api/goals/{goalId}/skill-gap/analyze`
  - `POST /api/goals/{goalId}/project-recommendation/recommend`
  - `POST /api/goals/{goalId}/path-recommendation/analyze`
  - `POST /api/jobs/plan-generation`、`/api/jobs/path-analysis`、
    `/api/jobs/progress-submission`
  - `GET /api/plans`、`GET /api/plans/{planId}`、
    `GET /api/plans/{planId}/tasks`、`GET /api/plans/{planId}/tasks/today`
  - `POST /api/plans/{planId}/versions/{version}/restore`
  - `POST /api/progress`、`GET /api/progress/{planId}`
  - `GET/PATCH /api/progress/{planId}/adaptive-schedule`
  - `GET /api/knowledge/documents`
  - `POST /api/knowledge/documents` 上传文档并创建知识入库 job
  - `PATCH /api/knowledge/documents/{documentId}/enabled`
  - `PATCH /api/knowledge/documents/{documentId}/settings`
  - `PATCH /api/knowledge/documents/{documentId}/metadata`
  - `PATCH /api/knowledge/documents/batch`
  - `GET /api/knowledge/documents/retrieval-preview/{goalId}`
  - `GET /api/knowledge/documents/strategy-compare`
  - `GET /api/agent-runs`、`GET /api/agent-runs/{runId}`

### Python Agent Service

- `agent-service/` 基于 Python `>=3.10`、FastAPI、Pydantic 2、httpx、Uvicorn。
- 当前 API 模块位于 `agent-service/app/api/`，已存在：
  - `health.py`
  - `profile.py`
  - `goal.py`
  - `skill_gap.py`
  - `project.py`
  - `plan.py`
  - `progress.py`
- 当前服务层位于 `agent-service/app/services/`，已存在：
  - `profile_analyzer.py`
  - `goal_decomposer.py`
  - `skill_gap_analyzer.py`
  - `project_recommender.py`
  - `plan_generator.py`
  - `progress_reviewer.py`
  - `plan_adjuster.py`
  - `agent_execution.py`
  - `deepseek_chat.py`
  - `model_retry.py`
  - `prompt_catalog.py`
  - `language.py`
- `DEEPSEEK_API_KEY` 未配置时仍需保持 mock fallback；真实模型调用继续遵守
  网络、超时、`429`、`5xx`、输出格式异常可重试，配置与权限类错误直接失败的原则。

## 当前重点模块

### 知识库

- 知识库不是占位模块，已经进入主流程。
- 前端 `frontend/src/app/knowledge/knowledge-workspace.tsx` 已支持：
  - 文档上传
  - 上传后异步入库轮询
  - 启用/停用
  - 搜索、按 scope/category/group/tag 筛选
  - 检索预览
  - 双目标策略对比
  - 目标知识偏好读取与更新
- 后端 `knowledge/` 模块已包含文档、chunk、上下文组装、策略对比、检索预览、
  批量修改等对象，不应再按“只有上传占位页”理解该模块。

### 成长路径与计划

- 目标详情页已有独立的成长路径工作台
  `frontend/src/app/goals/[goalId]/path-recommendation-workspace.tsx`。
- 当前链路不是直接“目标 -> 计划”；仓库已存在“路径分析 -> 基于路径生成计划”的独立入口。
- 计划详情页已存在版本面板 `frontend/src/app/plans/[planId]/plan-version-panel.tsx`，
  用于展示版本来源、差异摘要、受影响天数，并支持回滚。

### 进度调整

- 进度提交后除了 review，还存在自适应节奏控制接口
  `GET/PATCH /api/progress/{planId}/adaptive-schedule`。
- 写计划或任务时，不能再假设“计划只会初次生成一次，后续只做简单 carry-over”；
  当前代码已经朝“版本化、可调整、可回滚”演进。

## 当前工作区改动趋势

- `git status --short` 显示当前存在大量未提交改动，主要集中在三条线：
  - 知识库与目标知识偏好
  - 成长路径、计划版本、计划差异与回滚
  - 进度复盘、自适应节奏和 Agent 提示词/Schema 调整
- 另外，`frontend/src/app/knowledge/`、`frontend/src/app/plans/[planId]/`、
  `backend/knowledge/`、`backend/progress/`、`backend/learningplan/`、
  `agent-service/app/services/` 都处于活跃编辑中。
- 修改这些区域前，优先检查现有未提交改动的意图，避免把正在进行中的方案回退。

## 技术栈

- Frontend：Next.js `16.2.7`、React `19.2.4`、TypeScript `5`、Tailwind CSS `4`、
  Zod `4`、React Hook Form、Vitest
- Backend：Java `17`、Spring Boot `3.4.2`、Spring Security、Spring Data JPA、
  Redis、Flyway、PostgreSQL、Springdoc
- Agent Service：Python `>=3.10`、FastAPI `0.136.3`、Pydantic `2.13.4`、
  httpx `0.28.1`、Uvicorn `0.49.0`
- Infra：PostgreSQL `16`、Redis `7`、Docker Compose、GitHub Actions

## 服务端口

| 服务 | 目录 | 默认地址 |
| --- | --- | --- |
| Frontend | `frontend/` | `http://localhost:3000` |
| Backend | `backend/` | `http://localhost:8080` |
| Agent Service | `agent-service/` | `http://localhost:8000` |
| PostgreSQL | 根目录 `docker-compose.yml` | `localhost:5432` |
| Redis | 根目录 `docker-compose.yml` | `localhost:6379` |

## 语言约定

- 前端界面语言支持 `zh`/`en`，由 locale cookie 驱动。
- 目标输出语言仍是业务字段 `responseLanguage`，后端枚举位于
  `backend/src/main/java/com/aidevplanner/backend/goal/ResponseLanguage.java`，
  当前值只有 `zh` 和 `en`。
- Agent 输出继续遵守：
  - schema 字段名用英文
  - 自然语言内容按 `responseLanguage` 输出
- 新增 Agent 能力时，不要只切前端文案，必须同步考虑目标绑定语言。

## 启动与验证

- 推荐启动：项目根目录执行 `docker compose up --build`
- Docker 配置校验：`docker compose config`
- Backend：在 `backend/` 执行 `mvn test`
- Agent Service：在 `agent-service/` 执行 `pytest`、`ruff check .`
- Frontend：在 `frontend/` 执行
  `npm test`、`npm run lint`、`npm run typecheck`、`npm run format:check`、
  `npm run build`

## 注意事项

- 不要依赖 `backend/README.md` 和 `agent-service/README.md` 中的 Day 状态判断项目进度。
- 不提交真实 `.env`、JWT 密钥或任何 API Key。
- 当前仓库中存在 `.venv/`、`node_modules/`、`.next/`、`target/` 等本地产物；
  做工程整理时先确认是否属于本次任务范围。
- 需要描述“当前状态”时，以根目录 `README.md`、当前源码目录结构、
  控制器/页面入口、测试和 `git status` 为准。
