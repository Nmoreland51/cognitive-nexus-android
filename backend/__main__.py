import argparse
import os
from pathlib import Path

import uvicorn

from .engine import NexusEngine
from .server import create_app


def main():
    parser = argparse.ArgumentParser(description="Run the real Cognitive Nexus engine for the native Android app.")
    parser.add_argument("--engine-root", required=True, type=Path)
    parser.add_argument("--data-dir", type=Path, default=Path("backend-data"))
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", default=8000, type=int)
    args = parser.parse_args()
    data_dir = args.data_dir.resolve()
    token = os.environ.get("NEXUS_API_TOKEN", "")
    if len(token) < 32:
        parser.error("Set NEXUS_API_TOKEN to a random access token (at least 32 characters). Never commit it.")
    engine = NexusEngine(args.engine_root.resolve())
    uvicorn.run(create_app(engine, data_dir, token), host=args.host, port=args.port, access_log=False)


if __name__ == "__main__":
    main()
