from __future__ import annotations

import hashlib
import json
from pathlib import Path


def prepare_workspace(
    catalog_path: Path,
    workspace: Path,
    *,
    batch_size: int = 100,
) -> dict:
    """Freeze catalog entries into a deterministic manual-review workspace."""

    catalog_path = Path(catalog_path)
    workspace = Path(workspace)

    if batch_size <= 0:
        raise ValueError("batch_size must be positive")

    catalog_text = catalog_path.read_text(encoding="utf-8")
    payload = json.loads(catalog_text)

    if not isinstance(payload, dict):
        raise ValueError("Catalog payload must be a JSON object")

    entries_raw = payload.get("entries")
    if not isinstance(entries_raw, list):
        raise ValueError("Catalog 'entries' field must be a list")

    entries: list[dict[str, object]] = []
    for index, raw_entry in enumerate(entries_raw, start=1):
        if not isinstance(raw_entry, dict):
            raise ValueError(f"Entry #{index} must be an object")
        if not raw_entry.get("enabled"):
            continue
        for key in ("id", "text"):
            value = raw_entry.get(key)
            if not isinstance(value, str) or not value.strip():
                raise ValueError(f"Enabled entry #{index} missing required field '{key}'")
        entries.append(raw_entry)

    if workspace.exists():
        raise FileExistsError(f"Workspace {workspace} already exists; remove it before running prepare_workspace")

    total_entries = len(entries)
    total_batches = (total_entries + batch_size - 1) // batch_size if total_entries else 0
    next_batch_id = "batch-0001" if total_batches else None

    source_root = workspace / "source"
    batches_root = source_root / "batches"
    audit_root = workspace / "audit"
    audit_reports = audit_root / "batch_reports"
    reviews_root = workspace / "reviews"
    approved_root = workspace / "approved"
    docs_root = workspace / "docs"

    for directory in (
        workspace,
        source_root,
        batches_root,
        audit_root,
        audit_reports,
        reviews_root,
        approved_root,
        docs_root,
    ):
        directory.mkdir(parents=True, exist_ok=True)

    master_catalog_path = source_root / "master_catalog.jsonl"
    manifest_path = source_root / "manifest.json"
    progress_path = audit_root / "progress.json"
    source_hashes_path = audit_root / "source_hashes.json"

    master_rows: list[dict[str, object]] = []
    batches_metadata: list[dict[str, object]] = []
    source_hashes: dict[str, str] = {}
    global_seq = 0

    for batch_index in range(total_batches):
        batch_id = f"batch-{batch_index + 1:04d}"
        chunk = entries[batch_index * batch_size : batch_index * batch_size + batch_size]
        if not chunk:
            continue
        start_seq = global_seq + 1
        batch_path = batches_root / f"{batch_id}.source.jsonl"
        hasher = hashlib.sha256()

        with batch_path.open("w", encoding="utf-8", newline="\n") as handle:
            for batch_seq, entry in enumerate(chunk, start=1):
                global_seq += 1
                row = {
                    "global_seq": global_seq,
                    "batch_seq": batch_seq,
                    "idiom_id": entry["id"],
                    "text": entry["text"],
                    "pinyin": entry.get("pinyin", ""),
                    "short_explanation": entry.get("short_explanation", ""),
                }
                master_rows.append(row)
                line = json.dumps(row, ensure_ascii=False)
                handle.write(f"{line}\n")
                hasher.update(line.encode("utf-8"))
                hasher.update(b"\n")

        source_hashes[batch_id] = hasher.hexdigest()
        batches_metadata.append(
            {
                "batch_id": batch_id,
                "start_global_seq": start_seq,
                "end_global_seq": global_seq,
                "entry_count": len(chunk),
                "source_path": str(batch_path.resolve()),
            }
        )

    with master_catalog_path.open("w", encoding="utf-8", newline="\n") as master_handle:
        for row in master_rows:
            master_handle.write(f"{json.dumps(row, ensure_ascii=False)}\n")

    manifest = {
        "catalog_path": str(catalog_path.resolve()),
        "master_catalog_path": str(master_catalog_path.resolve()),
        "total_entries": total_entries,
        "total_batches": total_batches,
        "batch_size": batch_size,
        "batches": batches_metadata,
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    progress = {
        "total_entries": total_entries,
        "total_batches": total_batches,
        "completed_entries": 0,
        "kept_entries": 0,
        "filtered_entries": 0,
        "closed_batches": 0,
        "next_batch_id": next_batch_id,
        "batch_size": batch_size,
        "batches": {info["batch_id"]: "pending" for info in batches_metadata},
    }
    progress_path.write_text(json.dumps(progress, ensure_ascii=False, indent=2), encoding="utf-8")

    source_hashes_path.write_text(
        json.dumps(source_hashes, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    return {
        "total_entries": total_entries,
        "total_batches": total_batches,
        "next_batch_id": next_batch_id,
    }
