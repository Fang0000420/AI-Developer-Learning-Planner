# Backend

AI Developer Learning Planner 的 Java 后端服务，基于 Spring Boot 构建。
当前后端已经承担鉴权、业务状态持久化、异步任务编排、知识库管理、计划版本管理、
进度复盘和 Agent 调用整合，不再是早期只有用户与目标表的初始化骨架。

## 当前状态

- 技术栈：Java `17`、Spring Boot `3.4.2`、Spring Web、Spring Data JPA、
  Spring Security、Redis、Flyway、PostgreSQL、Springdoc、Lombok
- 默认端口：`8080`
- 构建工具：Maven
- 数据库迁移：Flyway
- 健康检查：`GET /api/health`
- Agent Service 地址配置：`agent-service.base-url`

## 后端职责

后端是整个系统的业务主入口，负责：

- 用户注册、登录和 JWT 鉴权
- 目标、画像、学习计划、每日任务、进度日志等核心业务数据持久化
- 调用 Python Agent 服务并保存结构化结果
- 为前端提供统一 REST API
- 管理异步 job、请求链路日志和 Agent Runs
- 维护知识库文档、检索上下文和目标知识偏好
- 管理计划版本、回滚、自适应节奏控制

## 主要模块

`src/main/java/com/aidevplanner/backend/` 下当前已经存在这些核心模块：

- `auth/`：注册、登录、JWT、认证上下文
- `goal/`：目标 CRUD、输出语言、目标知识偏好
- `profile/`：用户画像、目标画像快照、版本
- `goaldecomposition/`：目标拆解调用与结果保存
- `skillgap/`：技能差距分析
- `projectrecommendation/`：项目推荐
- `path/`：成长路径分析与推荐
- `learningplan/`：学习计划、每日任务、计划版本、版本回滚
- `progress/`：进度提交、复盘结果、自适应节奏控制
- `knowledge/`：知识文档、chunk、检索预览、策略对比、批量更新
- `asyncjob/`：异步任务创建、状态轮询、执行协调
- `agent/`：Agent Run 记录、查询与日志上下文
- `health/`：健康检查

## 已提供的 API

当前后端已经落地的接口族包括：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/health`
- `GET/POST/PUT/DELETE /api/goals`
- `GET/PATCH /api/goals/{goalId}/knowledge-preference`
- `GET/PATCH /api/profile`
- `GET/POST /api/progress`
- `GET/PATCH /api/progress/{planId}/adaptive-schedule`
- `GET /api/plans`
- `POST /api/plans/generate`
- `GET /api/plans/{planId}`
- `GET /api/plans/{planId}/tasks`
- `GET /api/plans/{planId}/tasks/today`
- `PUT /api/plans/{planId}/tasks/{taskId}/status`
- `POST /api/plans/{planId}/versions/{version}/restore`
- `POST /api/jobs/plan-generation`
- `POST /api/jobs/path-analysis`
- `POST /api/jobs/progress-submission`
- `GET /api/jobs/{jobId}`
- `GET /api/agent-runs`
- `GET /api/agent-runs/{runId}`
- `GET/POST/PATCH /api/knowledge/documents/...`
- `GET /api/knowledge/documents/retrieval-preview/{goalId}`
- `GET /api/knowledge/documents/strategy-compare`

此外，目标级分析能力也都已经通过后端暴露：

- `POST /api/goals/{goalId}/profile/analyze`
- `POST /api/goals/{goalId}/decomposition/decompose`
- `POST /api/goals/{goalId}/skill-gap/analyze`
- `POST /api/goals/{goalId}/project-recommendation/recommend`
- `POST /api/goals/{goalId}/path-recommendation/analyze`

## 配置

主要配置文件位于 `src/main/resources/application.yml`。当前关键配置包括：

```yaml
server.port=${BACKEND_PORT:8080}
spring.datasource.url=${SPRING_DATASOURCE_URL:...}
spring.data.redis.url=${REDIS_URL:redis://localhost:6379/0}
agent-service.base-url=${AGENT_SERVICE_BASE_URL:http://localhost:8000}
app.storage.knowledge-root=${KNOWLEDGE_STORAGE_ROOT:...}
```

常用环境变量：

```bash
BACKEND_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ai_planner
SPRING_DATASOURCE_USERNAME=ai_planner
SPRING_DATASOURCE_PASSWORD=ai_planner_dev_password
REDIS_URL=redis://localhost:6379/0
AGENT_SERVICE_BASE_URL=http://localhost:8000
KNOWLEDGE_STORAGE_ROOT=./storage/knowledge
JWT_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION_SECONDS=86400
```

## 本地依赖

后端本地开发至少需要：

- Java `17`
- PostgreSQL
- Redis
- 可选的 Python Agent Service

使用项目根目录 Docker Compose 启动依赖最方便：

```bash
docker compose up -d postgres redis
```

如果需要同时联调 Agent Service，也可以直接在项目根目录启动完整环境：

```bash
docker compose up --build
```

## 本地启动

```bash
cd backend
mvn spring-boot:run
```

启动后默认监听 `http://localhost:8080`。

## 健康检查

```bash
curl http://localhost:8080/api/health
```

期望返回：

```json
{"status":"UP","service":"ai-developer-learning-planner-backend"}
```

## 测试

```bash
mvn test
```

当前仓库中已经有覆盖控制器与服务层的测试，包括：

- 鉴权
- goals
- learning plans
- progress
- project recommendation
- skill gap
- agent runs
- async jobs
- knowledge documents

## 开发注意事项

- 新业务能力优先在后端收口，再由前端通过同源 API Route 代理访问
- 与 Agent Service 的调用日志、错误和 requestId 已经是系统行为的一部分，不要绕开现有调用封装
- `knowledge/`、`learningplan/`、`progress/`、`path/` 当前是高频演进区域，修改前先检查现有未提交改动
- `goal.ResponseLanguage` 当前只支持 `zh` 和 `en`，新增语言时需要同时考虑前端和 Agent schema
- 本仓库根目录与 `infra/docker-compose.yml` 都存在 Compose 入口，当前应优先使用根目录 `docker-compose.yml`
