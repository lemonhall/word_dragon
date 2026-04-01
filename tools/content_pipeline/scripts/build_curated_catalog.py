from __future__ import annotations

import argparse
import json
from pathlib import Path

from word_dragon_content.curated_pack import build_curated_catalog_payload


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    payload = build_curated_catalog_payload(
        catalog_path=args.catalog,
        workspace=args.workspace,
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(
        json.dumps(
            {
                "output": str(args.output.resolve()),
                "entry_count": payload["entry_count"],
                "legacy_schema_files": payload["manifest"]["manual_review"]["legacy_schema_files"],
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
