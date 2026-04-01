import hashlib
import json

import pytest
from pathlib import Path

from word_dragon_content.manual_review import prepare_workspace


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
