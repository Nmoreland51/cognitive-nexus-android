"""Opt-in live checks. Read-only by default; --chat explicitly tests local inference."""
import argparse
import logging
import secrets
import time
from pathlib import Path

from fastapi.testclient import TestClient

from .engine import NexusEngine
from .models import JobRequest, Options
from .server import create_app


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine-root", type=Path, required=True)
    parser.add_argument("--chat", action="store_true")
    parser.add_argument("--model", default="")
    parser.add_argument("--knowledge", action="store_true", help="Test actual ingestion/retrieval in an isolated fixture store")
    args = parser.parse_args()
    data_dir = Path("backend-data/live-http-check").resolve()
    engine = NexusEngine(args.engine_root.resolve())
    logging.getLogger("httpx").setLevel(logging.WARNING)
    token = secrets.token_urlsafe(32)
    with TestClient(create_app(engine, data_dir, token)) as client:
        client.headers["Authorization"] = "Bearer " + token
        assert client.get("/api/health").json()["api_version"] == 2

        def run(request):
            response = client.post("/api/jobs", json=request.model_dump(mode="json"))
            response.raise_for_status()
            key = response.json()["id"]
            deadline = time.monotonic() + 600
            while time.monotonic() < deadline:
                job = client.get("/api/jobs/" + key).json()
                if job["state"] == "failed":
                    raise RuntimeError(job["error"])
                if job["state"] == "succeeded":
                    return job["result"]
                time.sleep(.2)
            raise TimeoutError("Live job did not finish within 10 minutes.")

        for action in ["overview", "memory", "notes", "gallery", "tools", "profile", "workflows"]:
            result = run(JobRequest(action=action))
            print(f"PASS HTTP {action}: fields={','.join(result)}", flush=True)
            if action == "gallery" and result["images"]:
                media = client.get("/api/media/" + result["images"][0]["id"])
                assert media.status_code == 200 and media.content.startswith(b"\x89PNG")
                print("PASS HTTP authenticated gallery PNG", flush=True)
        if args.chat:
            request = JobRequest(action="chat", text="What is two plus two? Answer in one short sentence.",
                options=Options(provider="ollama", model=args.model, use_memory=False, use_knowledge_for_chat=False))
            result = run(request)
            assert len(client.get("/api/sessions/" + str(request.session_id)).json()["messages"]) == 2
            print(f"PASS HTTP chat/history: provider={result.get('provider')}, model={result.get('model')}, reply={result['reply']!r}", flush=True)
        if args.knowledge:
            # Real engine implementation, with test-only storage; never add fixtures to personal knowledge.
            from web_research_module import WebResearchModule
            engine.core._research_module = WebResearchModule(str(data_dir / "knowledge-fixture"), embedding_backend="hash")
            result = run(JobRequest(action="ingest", name="mobile-check.txt", text="Mobile adapter validation: the fictional observatory has a violet telescope named NexusTestScope."))
            assert result["status"] == "success" and result["chunks_count"] > 0
            result = run(JobRequest(action="knowledge", text="What is NexusTestScope?", options=Options(knowledge_use_ai=False)))
            assert result.get("results"), "Knowledge query did not return the ingested fixture"
            print("PASS HTTP file ingestion and actual hash-vector knowledge retrieval (isolated store)", flush=True)


if __name__ == "__main__":
    main()
