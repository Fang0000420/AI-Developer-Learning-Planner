# Agent Service

AI Developer Learning Planner 的 Python Agent 服务，基于 FastAPI 构建。
当前服务已经包含画像分析、目标拆解、技能差距分析、项目推荐、计划生成、
进度复盘和计划调整等结构化能力，不再是只有健康检查与占位 stub 的早期骨架。

## 当前状态

- 技术栈：Python `>=3.10`、FastAPI `0.136.3`、Pydantic `2.13.4`、
  httpx `0.28.1`、Uvicorn `0.49.0`
- 默认端口：`8000`
- 服务入口：`app.main:app`
- 健康检查：`GET /health`
- 观测能力：HTTP 请求日志中会带 requestId

## 服务职责

Agent Service 负责承接后端发起的 AI 分析任务，调用模型或 mock fallback，
并返回稳定的结构化 JSON 结果。当前职责包括：

- 能力画像分析
- 目标拆解
- 技能差距分析
- 项目推荐
- 学习计划生成
- 进度复盘
- 后续计划调整

## 当前目录

```text
.
├── app/
│   ├── api/
│   ├── schemas/
│   ├── services/
│   ├── config.py
│   ├── main.py
│   └── observability.py
├── tests/
├── pyproject.toml
└── requirements.txt
```

## API 模块

`app/api/` 当前已经包含：

- `health.py`
- `profile.py`
- `goal.py`
- `skill_gap.py`
- `project.py`
- `plan.py`
- `progress.py`

这些路由都已经在 `app/api/__init__.py` 中注册到统一的 `api_router`。

## 服务层

`app/services/` 当前已经包含：

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

其中：

- `deepseek_chat.py` 负责模型调用
- `model_retry.py` 负责错误分类与重试
- `prompt_catalog.py` 负责集中管理提示词模板
- `agent_execution.py` 负责执行期共用逻辑

## 环境变量

`app/config.py` 当前会读取这些关键变量：

```bash
DEEPSEEK_API_KEY=
DEEPSEEK_API_BASE_URL=https://api.deepseek.com
REDIS_URL=redis://localhost:6379/0

PROFILE_ANALYZER_MODEL=deepseek-v4-pro
PLAN_GENERATOR_MODEL=deepseek-v4-flash
PROJECT_RECOMMENDER_MODEL=deepseek-v4-flash

PROFILE_ANALYZER_TIMEOUT_SECONDS=30
GOAL_DECOMPOSER_TIMEOUT_SECONDS=30
SKILL_GAP_ANALYZER_TIMEOUT_SECONDS=30
PROJECT_RECOMMENDER_TIMEOUT_SECONDS=60
PLAN_GENERATOR_TIMEOUT_SECONDS=180
PROGRESS_REVIEWER_TIMEOUT_SECONDS=30
PLAN_ADJUSTER_TIMEOUT_SECONDS=60
```

如果没有配置 `DEEPSEEK_API_KEY`，服务必须继续可用，并返回 mock fallback。

## 模型调用约定

当前实现遵守以下原则：

- 网络错误、超时、`429`、`5xx`、模型输出格式异常可以重试
- 重试耗尽后允许回退到 mock fallback
- `400`、`401`、`403`、`404` 这类配置或权限问题应直接报错，不应伪装成正常 fallback
- 自然语言输出需要遵守目标绑定语言，当前业务语言是 `zh` 或 `en`

## 开发环境准备

### 创建虚拟环境

Windows PowerShell：

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -e ".[dev]"
```

macOS / Linux：

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
```

## 本地启动

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

如果只是普通运行，不需要热更新：

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

## 健康检查

```bash
curl http://localhost:8000/health
```

期望返回包含：

```json
{
  "service": "ai-developer-learning-planner-agent-service",
  "status": "UP",
  "version": "0.1.0"
}
```

## 测试与检查

```bash
pytest
ruff check .
```

当前测试已覆盖 schema 契约、提示词目录、模型重试逻辑以及部分 Agent 能力接口。

## 与后端联调

后端默认通过 `AGENT_SERVICE_BASE_URL` 指向本服务，常见本地联调值是：

```bash
AGENT_SERVICE_BASE_URL=http://localhost:8000
```

联调时通常至少还需要：

- Spring Boot backend
- PostgreSQL
- Redis

使用项目根目录 `docker compose up --build` 可以一次性启动完整链路。

## 开发注意事项

- 新增 Agent 能力时，优先在 `schemas/` 中先定义清晰的数据结构，再补服务层逻辑
- 不要把提示词散落到各个模块里，优先补充到 `prompt_catalog.py`
- `services/` 和 `schemas/` 当前有一批未提交改动，修改前先确认目标是继续现有方案还是单独修复
- 输出字段名保持英文 schema，不要因为中英文切换而改动 JSON 字段名
