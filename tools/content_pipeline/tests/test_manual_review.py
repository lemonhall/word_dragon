import hashlib
import json
from pathlib import Path

import pytest

from word_dragon_content.manual_review import prepare_workspace, validate_review_batch


SAMPLE_ENTRIES = [
    {
        "id": "idiom-00001",
        "text": "一世两清",
        "pinyin": "yī shì liǎng qīng",
        "short_explanation": "两兄弟官清声誉好。",
        "enabled": True,
    },
    {
        "id": "idiom-00002",
        "text": "一世之利",
        "pinyin": "yī shì zhī lì",
        "short_explanation": "天下的利益。",
        "enabled": True,
    },
    {
        "id": "idiom-00003",
        "text": "一世龙门",
        "pinyin": "yī shì lóng mén",
        "short_explanation": "一代名门。",
        "enabled": True,
    },
    {
        "id": "idiom-00004",
        "text": "一东一西",
        "pinyin": "yī dōng yī xī",
        "short_explanation": "一在东，一在西。",
        "enabled": True,
    },
    {
        "id": "idiom-00005",
        "text": "一个半个",
        "pinyin": "yī gè bàn gè",
        "short_explanation": "指数量很少。",
        "enabled": True,
    },
    {
        "id": "idiom-00006",
        "text": "一心一意",
        "pinyin": "yī xīn yī yì",
        "short_explanation": "指专心致志。",
        "enabled": False,
    },
]


def _setup_workspace(tmp_path: Path, *, batch_size: int = 5) -> Path:
    catalog_path = _write_catalog(tmp_path)
    workspace = tmp_path / "m5"
    prepare_workspace(
        catalog_path=catalog_path,
        workspace=workspace,
        batch_size=batch_size,
    )
    return workspace


def _read_source_rows(workspace: Path, batch_id: str = "batch-0001") -> list[dict[str, object]]:
    source_path = workspace / "source" / "batches" / f"{batch_id}.source.jsonl"
    return [json.loads(line) for line in source_path.read_text(encoding="utf-8").strip().splitlines()]


def _default_review_rows(workspace: Path, batch_id: str = "batch-0001") -> list[dict[str, object]]:
    source_rows = _read_source_rows(workspace, batch_id)
    return [
        {
            "global_seq": row["global_seq"],
            "idiom_id": row["idiom_id"],
            "text": row["text"],
            "decision": "keep",
            "note": "",
        }
        for row in source_rows
    ]


def _write_review(workspace: Path, batch_id: str, rows: list[dict[str, object]]) -> None:
    review_path = workspace / "reviews" / f"{batch_id}.review.jsonl"
    review_path.parent.mkdir(parents=True, exist_ok=True)
    with review_path.open("w", encoding="utf-8", newline="\n") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False))
            handle.write("\n")


def _write_catalog(tmp_path: Path) -> Path:
    payload = {
        "generated_at": "2026-04-01T00:00:00Z",
        "entries": SAMPLE_ENTRIES,
    }
    catalog_path = tmp_path / "idiom_catalog.json"
    catalog_path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    return catalog_path


def test_prepare_workspace_splits_enabled_entries_into_fixed_batches(tmp_path: Path):
    catalog_path = _write_catalog(tmp_path)
    workspace = tmp_path / "m5"

    report = prepare_workspace(
        catalog_path=catalog_path,
        workspace=workspace,
        batch_size=2,
    )

    assert report["total_entries"] == 5
    assert report["total_batches"] == 3
    assert report["next_batch_id"] == "batch-0001"

    batch_folder = workspace / "source" / "batches"
    assert (batch_folder / "batch-0001.source.jsonl").exists()
    assert (batch_folder / "batch-0003.source.jsonl").exists()

    first_lines = (batch_folder / "batch-0001.source.jsonl").read_text(
        encoding="utf-8"
    ).strip().splitlines()
    assert len(first_lines) == 2

    master_lines = (workspace / "source" / "master_catalog.jsonl").read_text(
        encoding="utf-8"
    ).strip().splitlines()
    assert len(master_lines) == 5
    assert '"idiom-00005"' in master_lines[-1]

    assert (workspace / "reviews").is_dir()
    assert (workspace / "approved").is_dir()
    assert (workspace / "docs").is_dir()

    progress = json.loads((workspace / "audit" / "progress.json").read_text(encoding="utf-8"))
    assert progress["batches"]["batch-0001"] == "pending"


def test_prepare_workspace_preserves_catalog_order_and_hashes(tmp_path: Path):
    catalog_path = _write_catalog(tmp_path)
    workspace = tmp_path / "m5"

    prepare_workspace(catalog_path=catalog_path, workspace=workspace, batch_size=200)

    manifest = json.loads(
        (workspace / "source" / "manifest.json").read_text(encoding="utf-8")
    )
    assert manifest["batches"][0]["start_global_seq"] == 1
    assert manifest["batches"][0]["end_global_seq"] == 5

    source_hashes = json.loads(
        (workspace / "audit" / "source_hashes.json").read_text(encoding="utf-8")
    )
    batch_path = workspace / "source" / "batches" / "batch-0001.source.jsonl"
    actual_hash = hashlib.sha256(batch_path.read_bytes()).hexdigest()
    assert source_hashes["batch-0001"] == actual_hash

    master = [
        json.loads(line) for line in (workspace / "source" / "master_catalog.jsonl").read_text(encoding="utf-8").strip().splitlines()
    ]
    assert master[0]["global_seq"] == 1
    assert master[1]["global_seq"] == 2
    manifest_path = Path(manifest["batches"][0]["source_path"])
    expected_path = (workspace / "source" / "batches" / "batch-0001.source.jsonl").resolve()
    assert manifest_path == expected_path


def test_prepare_workspace_rejects_existing_workspace(tmp_path: Path):
    catalog_path = _write_catalog(tmp_path)
    workspace = tmp_path / "m5"

    prepare_workspace(catalog_path=catalog_path, workspace=workspace, batch_size=2)
    manifest_before = (workspace / "source" / "manifest.json").read_text(encoding="utf-8")

    with pytest.raises(FileExistsError):
        prepare_workspace(catalog_path=catalog_path, workspace=workspace, batch_size=2)

    manifest_after = (workspace / "source" / "manifest.json").read_text(encoding="utf-8")
    assert manifest_before == manifest_after


def test_prepare_workspace_validates_enabled_entries(tmp_path: Path):
    invalid_payload = {
        "generated_at": "2026-04-01T00:00:00Z",
        "entries": [
            {
                "id": "idiom-00001",
                "text": "一世两清",
                "pinyin": "yī shì liǎng qīng",
                "short_explanation": "两兄弟官清声誉好。",
                "enabled": True,
            },
            {
                "text": "缺失编码",
                "pinyin": "quē",
                "short_explanation": "缺少 id 字段。",
                "enabled": True,
            },
        ],
    }
    catalog_path = tmp_path / "invalid_catalog.json"
    catalog_path.write_text(json.dumps(invalid_payload, ensure_ascii=False), encoding="utf-8")
    workspace = tmp_path / "m5"

    with pytest.raises(ValueError):
        prepare_workspace(catalog_path=catalog_path, workspace=workspace, batch_size=2)

    assert not workspace.exists()


@pytest.mark.parametrize(
    "payload",
    [
        "not-an-object",
        {"generated_at": "2026-04-01T00:00:00Z", "entries": "not a list"},
        {"generated_at": "2026-04-01T00:00:00Z", "entries": [1]},
    ],
)
def test_prepare_workspace_rejects_malformed_catalog(tmp_path: Path, payload):
    catalog_path = tmp_path / "bad_catalog.json"
    catalog_path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    workspace = tmp_path / "m5"

    with pytest.raises(ValueError):
        prepare_workspace(catalog_path=catalog_path, workspace=workspace, batch_size=2)

    assert not workspace.exists()


def test_validate_review_batch_rejects_missing_rows_and_invalid_decisions(tmp_path: Path):
    workspace = _setup_workspace(tmp_path, batch_size=2)
    rows = _default_review_rows(workspace)
    rows = rows[:1]
    rows[0]["decision"] = "maybe"
    _write_review(workspace, "batch-0001", rows)

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("条目数不一致" in error for error in errors)
    assert any("decision 非法" in error for error in errors)


def test_validate_review_batch_requires_reason_for_filter(tmp_path: Path):
    workspace = _setup_workspace(tmp_path, batch_size=2)
    rows = _default_review_rows(workspace)
    rows[0]["decision"] = "filter"
    rows[0]["note"] = "   "
    _write_review(workspace, "batch-0001", rows)

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("过滤原因不能为空" in error for error in errors)


def test_validate_review_batch_rejects_source_hash_tampering(tmp_path: Path):
    workspace = _setup_workspace(tmp_path, batch_size=2)
    rows = _default_review_rows(workspace)
    _write_review(workspace, "batch-0001", rows)

    source_path = workspace / "source" / "batches" / "batch-0001.source.jsonl"
    original_lines = source_path.read_text(encoding="utf-8").splitlines()
    tampered_row = json.loads(original_lines[0])
    tampered_row["text"] = "伪造词条"
    original_lines[0] = json.dumps(tampered_row, ensure_ascii=False)
    source_path.write_text("\n".join(original_lines) + "\n", encoding="utf-8", newline="\n")

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("源文件哈希已变化" in error for error in errors)


def test_validate_review_batch_missing_review_file(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("review 文件" in error for error in errors)


def test_validate_review_batch_missing_source_file(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    _write_review(workspace, "batch-0001", rows)
    (workspace / "source" / "batches" / "batch-0001.source.jsonl").unlink()

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("source 文件不存在" in error for error in errors)


def test_validate_review_batch_rejects_entry_count_mismatch(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    _write_review(workspace, "batch-0001", rows[:-1])

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("条目数不一致" in error for error in errors)


def test_validate_review_batch_rejects_invalid_decision_and_missing_note(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    rows[0]["decision"] = "reject"
    rows[1]["decision"] = "filter"
    rows[1]["note"] = "   "
    _write_review(workspace, "batch-0001", rows)

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("decision" in error for error in errors)
    assert any("过滤原因不能为空" in error for error in errors)


def test_validate_review_batch_rejects_entry_metadata_mismatch(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    rows[0]["global_seq"] += 1
    rows[1]["idiom_id"] = "idiom-99999"
    rows[1]["text"] = "新的文本"
    _write_review(workspace, "batch-0001", rows)

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("global_seq" in error for error in errors)
    assert any("词条标识" in error for error in errors)


def test_validate_review_batch_detects_hash_mismatch(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    rows[2]["decision"] = "filter"
    rows[2]["note"] = "冷僻理由"
    _write_review(workspace, "batch-0001", rows)

    source_hashes_path = workspace / "audit" / "source_hashes.json"
    hashes = json.loads(source_hashes_path.read_text(encoding="utf-8"))
    hashes["batch-0001"] = "deadbeef"
    source_hashes_path.write_text(json.dumps(hashes, ensure_ascii=False, indent=2), encoding="utf-8")

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("源文件哈希" in error for error in errors)


def test_validate_review_batch_missing_hash_manifest(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    _write_review(workspace, "batch-0001", rows)
    (workspace / "audit" / "source_hashes.json").unlink()

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("source_hashes.json 缺失" in error for error in errors)


def test_validate_review_batch_missing_batch_hash_registration(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    _write_review(workspace, "batch-0001", rows)

    source_hashes_path = workspace / "audit" / "source_hashes.json"
    source_hashes = json.loads(source_hashes_path.read_text(encoding="utf-8"))
    source_hashes.pop("batch-0001")
    source_hashes_path.write_text(
        json.dumps(source_hashes, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("batch-0001 在 audit/source_hashes.json 中缺失" in error for error in errors)


def test_validate_review_batch_detects_duplicate_global_seq(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    rows[1] = rows[0].copy()
    _write_review(workspace, "batch-0001", rows)

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert any("重复" in error for error in errors)


def test_validate_review_batch_accepts_valid_review(tmp_path: Path):
    workspace = _setup_workspace(tmp_path)
    rows = _default_review_rows(workspace)
    rows[3]["decision"] = "filter"
    rows[3]["note"] = "网络用户反馈偏僻。"
    _write_review(workspace, "batch-0001", rows)

    errors = validate_review_batch(workspace=workspace, batch_id="batch-0001")

    assert errors == []
