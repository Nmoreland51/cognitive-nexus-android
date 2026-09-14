"""Single-owner mobile API. Start using python -m backend --engine-root ..."""
import logging
import secrets
from concurrent.futures import ThreadPoolExecutor
from contextlib import asynccontextmanager
from pathlib import Path
from uuid import UUID

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.responses import FileResponse, JSONResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from .models import JobRequest, JobResponse
from .store import Store


def create_app(engine, data_dir: Path, token: str):
    if len(token) < 32 or not token.isascii():
        raise ValueError("NEXUS_API_TOKEN must be at least 32 ASCII characters.")
    store = Store(data_dir / "mobile.sqlite3")
    executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix="nexus-engine")

    @asynccontextmanager
    async def lifespan(app):
        yield
        executor.shutdown(wait=False, cancel_futures=True)

    app = FastAPI(title="Cognitive Nexus Mobile API", version="2.0.0", lifespan=lifespan,
                  docs_url=None, redoc_url=None, openapi_url=None)
    bearer = HTTPBearer(auto_error=False)

    def authorize(credentials: HTTPAuthorizationCredentials | None = Depends(bearer)):
        supplied = credentials.credentials if credentials else ""
        if not secrets.compare_digest(supplied.encode(), token.encode()):
            raise HTTPException(401, "Backend access token is missing or incorrect.", headers={"WWW-Authenticate": "Bearer"})

    @app.middleware("http")
    async def limit_body(request: Request, call_next):
        # Bound chunked bodies too; do not rely only on Content-Length.
        if request.method in ("POST", "PUT"):
            body = bytearray()
            async for chunk in request.stream():
                body.extend(chunk)
                if len(body) > 1_100_000:
                    return JSONResponse({"detail": "Request exceeds 1.1 MB."}, status_code=413)
            request._body = bytes(body)
        response = await call_next(request)
        response.headers["Cache-Control"] = "no-store"
        response.headers["X-Content-Type-Options"] = "nosniff"
        return response

    @app.get("/api/health", dependencies=[Depends(authorize)])
    def health():
        return {"ok": True, "api_version": 2, "engine": "NexusCore", "model_status": "Use overview; health is not an inference test."}

    def execute(request):
        key = str(request.request_id)
        store.update(key, "running")
        try:
            result = engine.execute(request, store)
            store.update(key, "succeeded", result=result)
        except Exception as exc:
            # Never return arbitrary engine exception strings: they may include credentials or paths.
            logging.error("Mobile job %s (%s) failed: %s", key, request.action, type(exc).__name__)
            message = str(exc) if isinstance(exc, PublicError) else "Engine operation failed. Check server diagnostics and installed dependencies."
            store.update(key, "failed", error=message)

    @app.post("/api/jobs", response_model=JobResponse, dependencies=[Depends(authorize)], status_code=202)
    def submit(request: JobRequest):
        if request.action in {"chat", "research", "web", "bloodhound", "knowledge", "ingest", "ingest_url", "note", "remember", "forget", "images"} and not request.text.strip():
            raise HTTPException(422, "Enter text before starting this operation.")
        if request.action not in {"ingest", "note", "save_profile"} and len(request.text) > 16000:
            raise HTTPException(422, "Text exceeds 16,000 characters.")
        try:
            if store.enqueue(request):
                executor.submit(execute, request)
        except ValueError as exc:
            raise HTTPException(409, str(exc)) from exc
        except OverflowError as exc:
            raise HTTPException(429, str(exc)) from exc
        return store.get(str(request.request_id))

    @app.get("/api/jobs/{key}", response_model=JobResponse, dependencies=[Depends(authorize)])
    def job(key: UUID):
        result = store.get(str(key))
        if result is None:
            raise HTTPException(404, "Task not found on this backend.")
        return result

    @app.get("/api/sessions", dependencies=[Depends(authorize)])
    def sessions():
        return {"sessions": store.sessions()}

    @app.get("/api/sessions/{key}", dependencies=[Depends(authorize)])
    def messages(key: UUID):
        return {"messages": store.messages(str(key))}

    @app.get("/api/media/{key}", dependencies=[Depends(authorize)])
    def media(key: str):
        path = engine.media_path(key)
        if path is None:
            raise HTTPException(404, "Image not found.")
        return FileResponse(path, media_type="image/png", filename=path.name)

    app.state.store = store
    return app


class PublicError(Exception):
    """Only developer-authored, credential-free messages may be sent to clients."""
