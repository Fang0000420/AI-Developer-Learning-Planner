from fastapi import FastAPI

from app.api import api_router
from app.config import APP_NAME, APP_VERSION


def create_app() -> FastAPI:
    app = FastAPI(title=APP_NAME, version=APP_VERSION)
    app.include_router(api_router)
    return app


app = create_app()
