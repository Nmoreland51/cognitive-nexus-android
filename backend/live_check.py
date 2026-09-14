"""Opt-in live checks. Read-only by default; --chat explicitly tests local inference."""
import argparse
from pathlib import Path

from .engine import NexusEngine
from .models import JobRequest, Options
from .store import Store


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--engine-root", type=Path, required=True)
    parser.add_argument("--chat", action="store_true")
    parser.add_argument("--model", default="")
    args = parser.parse_args()
    store = Store(Path("backend-data/live-check.sqlite3").resolve())
    engine = NexusEngine(args.engine_root.resolve())
    for action in ["overview", "memory", "notes", "gallery", "tools", "profile", "workflows"]:
        result = engine.execute(JobRequest(action=action), store)
        print(f"PASS {action}: fields={','.join(result)}", flush=True)
    if args.chat:
        result = engine.execute(JobRequest(action="chat", text="What is two plus two? Answer in one short sentence.",
            options=Options(provider="ollama", model=args.model, use_memory=False, use_knowledge_for_chat=False)), store)
        print(f"PASS chat: provider={result.get('provider')}, model={result.get('model')}, reply={result['reply']!r}", flush=True)


if __name__ == "__main__":
    main()
