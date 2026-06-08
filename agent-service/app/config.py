import os

APP_NAME = "ai-developer-learning-planner-agent-service"
APP_VERSION = "0.1.0"
HEALTH_STATUS = "UP"

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
DEEPSEEK_API_BASE_URL = os.getenv("DEEPSEEK_API_BASE_URL", "https://api.deepseek.com")
PROFILE_ANALYZER_MODEL = os.getenv("PROFILE_ANALYZER_MODEL", "deepseek-v4-pro")
PROFILE_ANALYZER_TIMEOUT_SECONDS = float(os.getenv("PROFILE_ANALYZER_TIMEOUT_SECONDS", "30"))
