"""Durable jobs and mobile chat history. A retry never repeats a completed action."""
import json
import sqlite3
import threading
from contextlib import contextmanager
from pathlib import Path


class Store:
    def __init__(self, path: Path):
        path.parent.mkdir(parents=True, exist_ok=True)
        self.path = path
        self.lock = threading.RLock()
        with self.db() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS jobs (
                  id TEXT PRIMARY KEY, action TEXT NOT NULL, request TEXT NOT NULL,
                  state TEXT NOT NULL, result TEXT, error TEXT,
                  created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
                CREATE TABLE IF NOT EXISTS messages (
                  id INTEGER PRIMARY KEY, session TEXT NOT NULL, role TEXT NOT NULL,
                  content TEXT NOT NULL, created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
            """)
            db.execute("UPDATE jobs SET state='failed', error=? WHERE state IN ('queued','running')",
                       ("Backend restarted before completion. Review saved outputs before retrying.",))

    @contextmanager
    def db(self):
        with self.lock:
            db = sqlite3.connect(self.path, timeout=10)
            db.row_factory = sqlite3.Row
            try:
                with db:
                    yield db
            finally:
                db.close()

    def get(self, key):
        with self.db() as db:
            row = db.execute("SELECT * FROM jobs WHERE id=?", (key,)).fetchone()
        if row is None:
            return None
        return dict(id=row["id"], action=row["action"], state=row["state"],
                    result=json.loads(row["result"]) if row["result"] else None, error=row["error"])

    def enqueue(self, request):
        key = str(request.request_id)
        payload = request.model_dump_json()
        with self.db() as db:
            old = db.execute("SELECT request FROM jobs WHERE id=?", (key,)).fetchone()
            if old:
                if old["request"] != payload:
                    raise ValueError("Request ID already used with different data.")
                return False
            pending = db.execute("SELECT count(*) FROM jobs WHERE state IN ('queued','running')").fetchone()[0]
            if pending >= 8:
                raise OverflowError("Backend queue is full. Wait for an active task to finish.")
            db.execute("INSERT INTO jobs(id,action,request,state) VALUES(?,?,?,'queued')",
                       (key, request.action, payload))
        return True

    def update(self, key, state, result=None, error=None):
        with self.db() as db:
            db.execute("UPDATE jobs SET state=?,result=?,error=? WHERE id=?",
                       (state, json.dumps(result) if result is not None else None, error, key))

    def messages(self, session):
        with self.db() as db:
            return [dict(row) for row in db.execute(
                "SELECT id,role,content FROM (SELECT * FROM messages WHERE session=? ORDER BY id DESC LIMIT 500) ORDER BY id", (session,))]

    def add(self, session, role, content):
        with self.db() as db:
            db.execute("INSERT INTO messages(session,role,content) VALUES(?,?,?)", (session, role, content))

    def sessions(self):
        with self.db() as db:
            return [dict(row) for row in db.execute("""
                SELECT session AS id, substr(min(CASE WHEN role='user' THEN content END),1,80) AS title,
                count(*) AS message_count, max(id) AS updated FROM messages
                GROUP BY session ORDER BY updated DESC LIMIT 100
            """)]
