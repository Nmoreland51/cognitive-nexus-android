"""Explicit calls into inspected original modules; no replacement AI or image stubs."""
import hashlib
import ipaddress
import json
import os
import socket
import sys
from pathlib import Path
from urllib.parse import urlparse

from .server import PublicError


class StrictRouter:
    """The desktop fallback is useful there, but must never masquerade as mobile AI."""
    def __init__(self, router):
        self.router = router

    def __getattr__(self, name):
        return getattr(self.router, name)

    def generate(self, request):
        result = self.router.generate(request)
        if not result.success or result.provider == "fallback":
            raise PublicError("No model completed the request. Check the selected provider on the server.")
        return result

    def stream(self, request, preferred_provider=None):
        chunks = list(self.router.stream(request, preferred_provider=preferred_provider))
        meta = self.router.last_stream_metadata
        if not meta.get("success") or meta.get("provider") == "fallback":
            raise PublicError("No model completed the request. Check the selected provider on the server.")
        yield from chunks


def public_url(url):
    parsed = urlparse(url)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname or parsed.username or parsed.password:
        raise PublicError("Enter a public http:// or https:// URL without credentials.")
    if parsed.port not in (None, 80, 443):
        raise PublicError("Only standard web ports are supported for URL ingestion.")
    try:
        addresses = socket.getaddrinfo(parsed.hostname, parsed.port or 443, type=socket.SOCK_STREAM)
    except OSError as exc:
        raise PublicError("The website hostname could not be resolved.") from exc
    if not addresses or any(not ipaddress.ip_address(item[4][0]).is_global for item in addresses):
        raise PublicError("Private, loopback, and link-local URLs cannot be ingested.")
    return url


class NexusEngine:
    def __init__(self, root: Path):
        if not (root / "modules/nexus_core.py").is_file():
            raise ValueError("Engine root must contain the original modules/nexus_core.py.")
        self.root = root.resolve()
        # Original modules use both source-relative and cwd-relative data paths.
        # Run one engine per process, sharing its existing data, never import Streamlit UI.
        os.chdir(self.root)
        sys.path.insert(0, str(self.root))
        from modules.nexus_core import NexusCore
        self.core = NexusCore(self.root)
        self.core.provider_router = StrictRouter(self.core.provider_router)

    def providers(self):
        order = [x for x in self.core.config.get("provider_order", []) if x != "fallback"]
        return [x.to_dict() for x in self.core.provider_router.detect_all(order)]

    def settings(self, options, require_model=False):
        from modules.chat_profile import load_chat_profile
        from nexus_router import RouterConfig
        choices = [x for x in self.providers() if x["available"] and (options.provider == "auto" or x["name"] == options.provider)]
        if options.model:
            choices = [x for x in choices if options.model in x.get("models", [])]
        if require_model and not choices:
            raise PublicError("No selected LLM is available. Start Ollama/load a model, or configure a server-side provider.")
        model = options.model or next((m for x in choices for m in x.get("models", [])), "")
        # Select one provider/model pair; do not silently substitute a paid provider on failure.
        order = [choices[0]["name"]] if choices else []
        return self.core.config | options.model_dump() | {
            "provider_order": order, "selected_model": model,
            "base_url": self.core.config.get("ollama_url", "http://127.0.0.1:11434"),
            "router_config": RouterConfig(default_model=model),
            "chat_profile": load_chat_profile(), "generation_timeout": 300.0,
            "show_sources": True, "enable_response_self_critic": True,
            "bloodhound_depth": options.depth, "bloodhound_max_results": options.max_results,
            "bloodhound_follow_links": options.follow_links,
            "enable_reality_research_agent": options.use_web_for_chat,
            "enable_bloodhound_search": options.use_web_for_chat,
        }

    def gallery(self):
        from modules.image_gen import list_generated_images
        items = list_generated_images(limit=100)
        for path in (self.root / "data/comfyui/outputs").glob("*.png"):
            items.append({"path": path, "name": path.name, "metadata": {}})
        result = []
        for item in items:
            path = Path(item["path"]).resolve()
            if not self.allowed_image(path):
                continue
            meta = item.get("metadata", {})
            result.append({"id": hashlib.sha256(str(path).encode()).hexdigest(), "name": path.name,
                           "prompt": str(meta.get("prompt", "")), "provider": str(meta.get("provider", "unknown")),
                           "model": str(meta.get("model", "")), "bytes": path.stat().st_size})
        return result

    def allowed_image(self, path):
        roots = ["data/images/generated", "ai_system/knowledge_bank/images", "generated_images", "data/comfyui/outputs"]
        return path.is_file() and path.suffix.lower() == ".png" and any(path.is_relative_to((self.root / r).resolve()) for r in roots)

    def media_path(self, key):
        if len(key) != 64 or any(c not in "0123456789abcdef" for c in key):
            return None
        for folder in ["data/images/generated", "ai_system/knowledge_bank/images", "generated_images", "data/comfyui/outputs"]:
            for candidate in (self.root / folder).glob("*.png"):
                path = candidate.resolve()
                if self.allowed_image(path) and hashlib.sha256(str(path).encode()).hexdigest() == key:
                    return path
        return None

    def execute(self, req, store):
        from modules import research
        from modules.context_manager import load_user_profile_summary, remember_user_fact, forget_user_fact
        from modules.image_gen import detect_image_providers, ImageGenerationRequest, IMAGE_STYLE_OPTIONS
        from modules.project_status import get_project_inventory, list_project_tools
        from modules.chat_profile import load_chat_profile, save_chat_profile
        action, opts = req.action, req.options
        session = str(req.session_id)

        if action in {"overview", "diagnostics"}:
            self.core.provider_router.invalidate_status_cache()
            inventory = get_project_inventory()
            # Return selected metadata, not .env, raw logs, credentials or arbitrary paths.
            return {"providers": [{k: x[k] for k in ("name", "available", "models")} for x in self.providers()],
                    "provider_note": "Configured/detected only. A successful chat verifies inference.",
                    "counts": {k: inventory.get(k, 0) for k in ("generated_images", "research_sources", "research_reports", "knowledge_notes", "user_facts")},
                    "image_providers": [{k: x.get(k) for k in ("name", "label", "available", "implemented")} for x in detect_image_providers()],
                    "comfyui_available": self.core.comfyui.detect().available,
                    "last_chat": {k: self.core.last_provider_result.get(k) for k in ("provider", "model", "success", "elapsed", "timings")},
                    "image_styles": list(IMAGE_STYLE_OPTIONS)}
        if action == "chat":
            settings = self.settings(opts, require_model=True)
            history = store.messages(session)
            self.core.last_provider_result = {}
            reply = self.core.generate_chat_response(req.text, history, settings)
            meta = self.core.last_provider_result
            if not reply or meta.get("provider") == "fallback" or meta.get("success") is False:
                raise PublicError("The engine did not return a successful response. No assistant message was saved.")
            store.add(session, "user", req.text)
            store.add(session, "assistant", reply)
            return {"reply": reply, "provider": meta.get("provider", "engine"), "model": meta.get("model", ""),
                    "messages": store.messages(session)}
        if action in {"research", "web", "bloodhound"}:
            settings = self.settings(opts, require_model=opts.summarize)
            if action == "research":
                from modules.reality_research_agent import ResearchRequest
                report = self.core.run_reality_research(ResearchRequest(
                    query=req.text, depth=opts.depth, max_sources=opts.max_results,
                    follow_links=opts.follow_links, save_to_memory=opts.save_to_memory,
                    use_ai_summary=opts.summarize, save_report=True), settings)
                result = report.to_dict()
                result.pop("saved_paths", None)
                result["report"] = report.to_markdown()
                return result
            if action == "bloodhound":
                # That engine callback has no separate summary flag. Require a real provider.
                result = self.core.run_bloodhound_search(req.text, self.settings(opts, require_model=True))
            else:
                result = self.core.run_web_research(req.text, settings, max_results=opts.max_results,
                    scrape_pages=True, summarize_with_ai=opts.summarize, save_locally=True, save_to_memory=opts.save_to_memory)
            return self.json_result(result)
        if action == "knowledge":
            return self.json_result(self.core.answer_knowledge(req.text, self.settings(opts, opts.knowledge_use_ai), top_k=opts.max_results))
        if action == "ingest":
            if Path(req.name).suffix.lower() not in {".txt", ".md", ".json", ".csv"}:
                raise PublicError("Upload UTF-8 .txt, .md, .json, or .csv files (up to 1 MB).")
            return self.checked(research.ingest_text(self.core.get_research_module(), name=Path(req.name).name, text=req.text, source_type="upload"))
        if action == "ingest_url":
            # Explicit redirect validation; no requests to private machines on the LAN.
            import requests
            from bs4 import BeautifulSoup
            url = req.text
            for _ in range(5):
                public_url(url)
                with requests.get(url, timeout=(5, 20), allow_redirects=False, stream=True) as response:
                    if response.is_redirect:
                        from urllib.parse import urljoin
                        url = urljoin(url, response.headers["Location"])
                        continue
                    response.raise_for_status()
                    data = bytearray()
                    for chunk in response.iter_content(65536):
                        data.extend(chunk)
                        if len(data) > 1_000_000:
                            raise PublicError("Website exceeds the 1 MB ingestion limit.")
                    soup = BeautifulSoup(bytes(data), "html.parser")
                    for tag in soup(["script", "style"]):
                        tag.decompose()
                    return self.checked(research.ingest_text(self.core.get_research_module(), name=url, text=soup.get_text(" ", strip=True), source_type="url"))
            raise PublicError("Too many website redirects.")
        if action == "note":
            return self.checked(research.save_knowledge_note(self.core.get_research_module(), title=req.name, text=req.text, tags=req.tags, ingest=opts.save_to_memory))
        if action == "notes":
            return {"notes": [{k: v for k, v in n.items() if k != "path"} for n in research.list_knowledge_notes(limit=100)]}
        if action == "memory":
            memory = self.core.get_adaptive_memory()
            return {"profile": load_user_profile_summary(limit=60), "adaptive_memory_available": memory is not None,
                    "candidate_count": len(getattr(memory, "memory_candidates", [])) if memory else None,
                    "messages": len(store.messages(session))}
        if action == "remember":
            return self.checked(remember_user_fact(req.text))
        if action == "forget":
            return self.checked(forget_user_fact(req.text))
        if action == "images":
            if opts.style not in IMAGE_STYLE_OPTIONS:
                raise PublicError("Select a supported image style.")
            result = self.core.generate_image(ImageGenerationRequest(prompt=req.text, negative_prompt=opts.negative_prompt,
                width=opts.width, height=opts.height, steps=opts.steps, seed=opts.seed, num_images=opts.num_images,
                provider=opts.image_provider, style=opts.style))
            if not result.get("success"):
                raise PublicError("Image generation failed. Start Automatic1111 with --api or configure local Diffusers on the server.")
            return {"provider": result.get("provider"), "images": self.gallery(), "generated_count": len(result.get("saved", []))}
        if action == "gallery":
            return {"images": self.gallery()}
        if action == "workflows":
            return {"workflows": [p.name for p in (self.root / "data/comfyui/workflows").glob("*.json")]}
        if action == "comfyui":
            folder = (self.root / "data/comfyui/workflows").resolve()
            path = (folder / req.name).resolve()
            if path.parent != folder or path.suffix != ".json" or not path.is_file():
                raise PublicError("Select a saved ComfyUI API workflow from this backend.")
            result = self.core.run_comfyui_workflow(workflow=json.loads(path.read_text(encoding="utf-8")), prompt=req.text, negative_prompt=opts.negative_prompt)
            if not result.success:
                raise PublicError("ComfyUI workflow failed. Check the saved workflow and the server's installed nodes/models.")
            return {"prompt_id": result.prompt_id, "images": self.gallery()}
        if action in {"profile", "save_profile"}:
            profile = load_chat_profile()
            fields = ["user_name", "assistant_name", "persona_summary", "tone_notes", "style_notes", "additional_instructions"]
            if action == "save_profile":
                try:
                    values = json.loads(req.text)
                    if not isinstance(values, dict) or set(values) - set(fields):
                        raise ValueError()
                    for key, value in values.items():
                        if not isinstance(value, str) or len(value) > 4000:
                            raise ValueError()
                        setattr(profile, key, value)
                except (ValueError, TypeError) as exc:
                    raise PublicError("Invalid persona fields (maximum 4,000 characters each).") from exc
                profile = save_chat_profile(profile)
            return {key: getattr(profile, key) for key in fields}
        if action == "tools":
            return {"tools": list_project_tools(), "note": "Inventory only, as in the desktop app. Arbitrary server commands are not exposed to phones.",
                    "learning_logs": [p.name for p in (self.root / ".learnings").glob("*.md")]}
        raise PublicError("Unsupported operation.")

    @staticmethod
    def json_result(result):
        # Drop server storage paths from results; do not dump arbitrary objects/PIL images.
        def clean(value):
            if isinstance(value, dict):
                return {k: clean(v) for k, v in value.items() if k not in {"path", "saved_paths", "file_path", "metadata_path", "raw_history"}}
            if isinstance(value, list):
                return [clean(v) for v in value]
            if isinstance(value, Path):
                return value.name
            return value
        return clean(result)

    def checked(self, result):
        if result.get("status") == "error" or result.get("success") is False:
            raise PublicError("The engine could not save this content. Check input and backend storage/dependencies.")
        return self.json_result(result)
