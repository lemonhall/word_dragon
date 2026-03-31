from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

from word_dragon_content.catalog_filter import build_catalog, load_character_frequency, load_raw_idioms
from word_dragon_content.source_manifest import (
    build_source_manifest,
    raw_character_frequency_path,
    raw_dictionary_path,
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--strict", action="store_true")
    args = parser.parse_args()

    manifest = build_source_manifest()
    catalog = build_catalog(
        load_raw_idioms(raw_dictionary_path()),
        load_character_frequency(raw_character_frequency_path()),
        strict=args.strict,
    )

    payload = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "manifest": manifest,
        "entry_count": len(catalog),
        "entries": catalog,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"catalog entries: {len(catalog)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
