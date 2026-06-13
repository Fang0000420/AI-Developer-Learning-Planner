# AGENTS.md

面向后续 coding agent 的短交接文档。历史阶段流水已归档到
`docs/archive/AGENTS-2026-06-10.md`，每日任务细节继续参考 `Plan/`。

## 当前状态

- 当前日期: 2026-06-10
- 当前进度: Day 20 已完成，Day 21 项目展示待开始
- 当前优化顺序:
  1. 任务 1: 中英文前端页面与中英文 Agent 提示词
  2. 任务 2: LongChain 增强，任务 1 完成后再澄清具体方案
- 运行口径: 开发、启动、测试和验收默认在服务器
  `/home/AI-Developer-Learning-Planner` 执行；本机主要用于文件编辑和 Git 辅助。

## 技术栈

- Frontend: Next.js 16, React 19, TypeScript, Tailwind CSS 4, Vitest
- Backend: Java 17, Spring Boot 3.4, Spring Web, Spring Data JPA, Spring Security, Flyway
- Agent Service: Python, FastAPI, Uvicorn, Pydantic, httpx
- Data/Infra: PostgreSQL 16, Redis 7, Docker Compose, GitHub Actions

## 服务端口

| 服务 | 目录 | 默认地址 |
| --- | --- | --- |
| Frontend | `frontend/` | `http://localhost:3000` |
| Backend | `backend/` | `http://localhost:8080` |
| Agent Service | `agent-service/` | `http://localhost:8000` |
| PostgreSQL | `infra/` | `localhost:5432` |
| Redis | `infra/` | `localhost:6379` |

## 语言约定

- 前端默认中文。
- 右上角 `CN/EN` 按钮通过 cookie 切换界面语言。
- Agent 输出语言为目标级配置，字段为 `responseLanguage`，取值 `zh` 或 `en`。
- 创建目标时输出语言默认跟随当前界面语言，用户可手动切换。
- 后续 Profile Analyzer、Goal Decomposer、Skill Gap Analyzer、Project Recommender、
  Plan Generator、Progress Reviewer 和 Plan Adjuster 都必须使用目标绑定的输出语言。
- Agent JSON 字段名保持英文 schema 不变，字段值中的自然语言按 `responseLanguage` 输出。

## 验证命令

- Backend: `mvn test`
- Agent Service: `ruff check .` 和 `pytest`
- Frontend: `npm test`、`npm run lint`、`npm run typecheck`、
  `npm run format:check`、`npm run build`
- Docker 配置: `docker compose config` 和
  `docker compose -f infra/docker-compose.yml config`

## 注意事项

- 不提交真实 `.env` 或任何密钥。
- `DEEPSEEK_API_KEY` 未配置时 Agent 服务必须继续使用 mock fallback。
- 真实模型调用仍按错误类型处理：网络、超时、429、5xx 和输出格式问题可重试；
  400、401、403、404 等配置或权限问题直接报错。
- 历史 Agent 输出不自动翻译；重新触发 Agent run 时才使用目标绑定语言。
- 任务 2 的 LongChain 还未定义，不要在任务 1 中顺手实现。
