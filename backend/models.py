from typing import Literal
from uuid import UUID, uuid4

from pydantic import BaseModel, ConfigDict, Field


class Options(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    provider: Literal["auto", "ollama", "openai", "anthropic", "huggingface_local"] = "auto"
    model: str = Field(default="", max_length=200)
    use_memory: bool = True
    use_knowledge_for_chat: bool = True
    use_web_for_chat: bool = False
    auto_precision_mode: bool = True
    knowledge_use_ai: bool = False
    summarize: bool = False
    save_to_memory: bool = False
    depth: Literal["Quick", "Standard", "Deep"] = "Standard"
    max_results: int = Field(default=5, ge=1, le=25)
    follow_links: bool = False
    style: str = Field(default="realistic", max_length=40)
    image_provider: Literal["auto", "automatic1111", "diffusers_local"] = "auto"
    negative_prompt: str = Field(default="", max_length=4000)
    width: int = Field(default=512, ge=256, le=1024, multiple_of=64)
    height: int = Field(default=512, ge=256, le=1024, multiple_of=64)
    steps: int = Field(default=25, ge=1, le=50)
    seed: int | None = Field(default=None, ge=0, le=4294967295)
    num_images: int = Field(default=1, ge=1, le=4)


class JobRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    request_id: UUID = Field(default_factory=uuid4)
    session_id: UUID = Field(default_factory=uuid4)
    action: Literal[
        "overview", "chat", "research", "web", "bloodhound", "knowledge",
        "ingest", "ingest_url", "note", "notes", "memory", "remember", "forget",
        "images", "gallery", "diagnostics", "tools", "profile", "save_profile", "workflows", "comfyui",
    ]
    text: str = Field(default="", max_length=1_000_000)
    name: str = Field(default="", max_length=200)
    tags: str = Field(default="", max_length=500)
    options: Options = Field(default_factory=Options)


class JobResponse(BaseModel):
    id: str
    action: str
    state: Literal["queued", "running", "succeeded", "failed"]
    result: dict | None = None
    error: str | None = None
