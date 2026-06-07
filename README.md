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

## 技术栈

| 模块              | 技术                                                                                                                                        |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| 前端              | Next.js, React, TypeScript, Tailwind CSS, shadcn/ui, React Hook Form, Zod, TanStack Query                                                   |
| Java 后端         | Spring Boot, Spring Web, Spring Security, Spring Data JPA, Hibernate, Flyway, Springdoc OpenAPI, MapStruct, Lombok, JUnit 5, Testcontainers |
| Python Agent 服务 | FastAPI, Pydantic, LangGraph, OpenAI Agents SDK, LangChain, Celery, SQLAlchemy, Pytest, Ruff                                                |
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

## 本地启动

当前仓库处于 Day 02 后端初始化阶段，`backend/` 已具备 Spring Boot + Maven 基础骨架、PostgreSQL/JPA/Flyway 配置和 `/api/health` 接口。首批业务表 migration 会在 Day 02 后续任务中继续补齐。

计划中的本地启动方式：

```bash
# 1. 复制环境变量模板
cp .env.example .env

# 2. 启动 PostgreSQL 和 Redis
docker compose -f infra/docker-compose.yml up postgres redis

# 3. 启动 Java 后端
cd backend
mvn spring-boot:run

# 4. 验证后端健康检查
curl http://localhost:8080/api/health

# 5. 启动 Python Agent 服务
cd agent-service
uvicorn app.main:app --reload --port 8000

# 6. 启动前端
cd frontend
npm run dev
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
