from fastapi import FastAPI

from app.api import api_router
from app.config import APP_NAME, APP_VERSION
from app.observability import request_logging_middleware


def create_app() -> FastAPI:
    app = FastAPI(title=APP_NAME, version=APP_VERSION)
    app.middleware("http")(request_logging_middleware)
    app.include_router(api_router)
    return app


app = create_app()
