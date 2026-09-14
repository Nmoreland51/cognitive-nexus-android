# Mobile API v2 — implemented adapter, not the old demo server

The inspected original `fullstack-local/backend/app.py` exposes only health, direct Ollama chat,
and a **placeholder** image endpoint. It is NOT suitable for full native feature parity.
Do not call its image endpoint. The previous discovery document describes v1, not this adapter.

`backend/server.py` now implements the following routes. `python -m unittest backend.test_api -v`
passed 10 contract/security tests on 2026-09-13 before Android v2 networking was added.
These tests use a test-only recording engine, not fabricated production responses.
Original-engine runtime verification is separately recorded in ANDROID_PORT_STATUS.md.

All routes require `Authorization: Bearer <NEXUS_API_TOKEN>` (at least 32 random ASCII characters).
No credentials are compiled into Android. This is a **single-owner** API sharing the original
engine's local knowledge and profile. It is not a multi-user hosting service.

| Route | Behavior |
|---|---|
| GET /api/health | `{ok:true,api_version:2,engine:"NexusCore",model_status:...}`; NOT an inference test |
| POST /api/jobs | Validate and queue a request; 202 JobResponse |
| GET /api/jobs/{UUID} | JobResponse, or 404 |
| GET /api/sessions | `{sessions:[{id,title,message_count,updated}]}` |
| GET /api/sessions/{UUID} | `{messages:[{id,role,content}]}`; latest 500, chronological |
| GET /api/media/{sha256} | Authenticated PNG from constrained engine gallery roots; never an arbitrary path |

Job request: `{request_id:UUID,session_id:UUID,action:string,text:"",name:"",tags:"",options:{...}}`.
Android always sends both UUIDs. Retrying the identical request_id and payload is idempotent;
reuse with different data returns 409. Max 8 queued/running jobs; full queue returns 429.
Body limit 1.1 MB (including chunked uploads). Files are UTF-8 text in the JSON `text` field.
JobResponse: `{id,action,state:"queued"|"running"|"succeeded"|"failed",result:object|null,error:string|null}`.
Poll until terminal state. Results and chat history persist in SQLite. A server restart marks
unfinished jobs failed; it never automatically repeats an operation that may have saved data.

## Actions mapped to inspected engine functions

| Action | Original function |
|---|---|
| overview / diagnostics | NexusCore providers, project_status.get_project_inventory, image provider detection |
| chat | NexusCore.generate_chat_response with RouterConfig, ChatProfile and saved session history |
| research | NexusCore.run_reality_research(ResearchRequest) |
| web | NexusCore.run_web_research |
| bloodhound | NexusCore.run_bloodhound_search |
| knowledge | NexusCore.answer_knowledge |
| ingest | modules.research.ingest_text (.txt/.md/.json/.csv, max 1 MB) |
| ingest_url | bounded public-URL fetch + HTML extraction + ingest_text |
| note / notes | save_knowledge_note / list_knowledge_notes |
| memory / remember / forget | context_manager.load_user_profile_summary / remember_user_fact / forget_user_fact |
| images / gallery | NexusCore.generate_image / image_gen.list_generated_images |
| workflows / comfyui | saved API workflow names / NexusCore.run_comfyui_workflow |
| profile / save_profile | chat_profile.load_chat_profile / save_chat_profile (six allowlisted style fields only) |
| tools | project_status.list_project_tools; inventory, NOT remote command execution |

Options are validated in `backend/models.py`. Real provider/model selection only; fallback text
is rejected by StrictRouter. No automatic cloud provider failover when a selected local model fails.
Research can run without AI synthesis; source extraction/heuristic verdicts are engine results,
not LLM answers. `summarize` opts into model synthesis. Bloodhound requires a real model.

No WebView, Streamlit embedding, mock provider, arbitrary shell endpoint, or API keys in the APK.
URL checks reject private/loopback/link-local addresses and validate redirects; the original research
engine still fetches external pages. Use a private trusted network or authenticated HTTPS reverse
proxy with an egress firewall. Do not expose this single-owner research service to untrusted users;
DNS rebinding and upstream tool behavior are not a public multi-tenant security boundary.
