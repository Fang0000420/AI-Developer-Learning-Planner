# Frontend

AI Developer Learning Planner 的前端应用，基于 Next.js App Router 构建。
当前前端已经不是项目初始化骨架，而是承担登录、目标管理、画像分析、成长路径、
学习计划、每日任务、知识库和 Agent 运行记录展示的完整界面层。

## 当前状态

- 技术栈：Next.js `16.2.7`、React `19.2.4`、TypeScript、Tailwind CSS `4`、
  React Hook Form、Zod、Vitest
- 默认端口：`3000`
- 打包方式：`standalone`
- 与后端的交互方式：
  - 浏览器优先请求同源的 Next.js API Route
  - Next.js 服务端再转发到 Spring Boot 后端
  - 登录态通过 cookie 保存在前端域名下

## 已实现页面

当前 `src/app/` 已存在以下核心页面和工作台：

- `/login`：注册与登录
- `/goals`：目标列表
- `/goals/new`：创建目标
- `/goals/[goalId]`：目标详情
  - 能力画像面板
  - 目标拆解面板
  - 技能差距面板
  - 项目推荐面板
  - 成长路径工作台
- `/plans`：学习计划列表
- `/plans/[planId]`：学习计划详情与版本面板
- `/plans/[planId]/today`：今日任务与进度提交
- `/tasks/today`：今日任务聚合入口
- `/profile`：个人画像页
- `/knowledge`：知识库工作台
- `/knowledge/[documentId]`：知识文档详情
- `/agent-runs`：Agent 运行记录列表
- `/agent-runs/[runId]`：Agent 运行详情

## 前端职责

前端负责界面渲染、表单校验、交互反馈和同源代理，不直接访问数据库，也不直接从浏览器调用
Python Agent 服务。当前已经接入的业务链路包括：

- 注册、登录、退出
- 目标创建、查询、编辑、删除
- 目标画像分析、目标拆解、技能差距分析、项目推荐
- 成长路径分析与基于路径生成学习计划
- 计划详情、按天任务查看、任务状态更新
- 进度提交与历史进度查看
- 计划版本展示与版本回滚
- 知识文档上传、启用/停用、元数据修改、批量更新
- 检索预览、策略对比、目标知识偏好配置
- Agent Runs 列表与详情查询

## API Route

前端的同源 API Route 位于 `src/app/api/`，当前已经覆盖：

- `auth/`：登录、注册、退出
- `goals/`：目标与目标下的分析动作
- `plans/`：计划、任务、版本回滚
- `progress/`：进度提交与进度查询
- `jobs/`：异步 job 触发与轮询
- `knowledge/`：知识库文档、检索预览、策略对比
- `agent-runs/`：Agent 运行记录
- `profile/`：个人画像
- `locale/`：界面语言切换
- `backend-health/`：后端健康检查

这种结构的目的是把浏览器访问统一收口到前端域名，减少跨域、认证头和 cookie 处理的复杂度。

## 语言与认证

- 界面语言支持 `zh` 和 `en`
- 右上角 `CN/EN` 开关通过 `/api/locale` 写入 cookie 后刷新页面
- 登录成功后，Next.js API Route 会把后端返回的 token 写入前端 cookie
- 需要鉴权的页面和 API Route 都应优先复用现有的认证工具，而不是各自重新拼接 token 逻辑

## 本地启动

### 安装依赖

```bash
npm ci
```

### 开发模式

```bash
npm run dev
```

如果需要监听局域网或容器内地址：

```bash
npm run dev:server
```

默认访问地址为 `http://localhost:3000`。

### 生产模式验证

```bash
npm run build
npm run start:server
```

通过公网或反向代理验证页面时，优先使用生产模式。`next dev` 会打开
`/_next/webpack-hmr` 的热更新 WebSocket，网络链路不完整时控制台可能出现
开发态专用报错，但这不代表页面主体不可用。

## 环境变量

前端会读取根目录 `.env` 或 `.env.example` 中的相关变量。最重要的是：

```bash
FRONTEND_BASE_URL=http://localhost:3000
BACKEND_BASE_URL=http://localhost:8080
NEXT_PUBLIC_BACKEND_API_BASE_URL=http://localhost:8080
AUTH_COOKIE_SECURE=false
```

说明：

- `BACKEND_BASE_URL` 供 Next.js 服务端 API Route 转发后端请求
- `NEXT_PUBLIC_BACKEND_API_BASE_URL` 主要用于前端展示或某些需要公开感知后端地址的场景
- `AUTH_COOKIE_SECURE` 在 HTTP 本地开发时通常保持 `false`，部署到 HTTPS 后再改成 `true`

## 健康检查

前端提供同源接口 `/api/backend-health`，它会由 Next.js 服务端去请求后端
`GET /api/health`。在联调前，先确认后端健康检查能正常返回：

```bash
curl http://localhost:8080/api/health
```

## 常用命令

```bash
npm run dev
npm run dev:server
npm run build
npm run start
npm run start:server
npm run test
npm run lint
npm run typecheck
npm run format
npm run format:check
```

## 测试与检查

提交前至少运行：

```bash
npm run test
npm run lint
npm run typecheck
npm run format:check
npm run build
```

## 开发注意事项

- 新页面优先放在 `src/app/`，并沿用现有的 App Router 目录结构
- 新的浏览器请求优先通过 `src/app/api/` 增加同源代理，而不是直接在客户端请求后端
- 已有页面中，`goals/[goalId]`、`plans/[planId]`、`plans/[planId]/today`、
  `knowledge/`、`profile/` 都处于高频演进区域，修改前先确认当前未提交改动
- 当前仓库已经存在中英文本地化，不要新增只支持单语言的新文案或新组件
