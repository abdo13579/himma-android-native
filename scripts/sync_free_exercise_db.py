#!/usr/bin/env python3
"""Sync & normalize exercises + compressed local WebP frames from yuhonas/free-exercise-db.

Source dataset (Public Domain / Unlicense):
  https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json
  https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/<id>/<0|1>.jpg

Outputs:
  app/src/main/assets/exercises_seed.json
  app/src/main/assets/exercises/<id>/0.webp & 1.webp

Design choices for Himma:
  - Normalizes upstream muscles into Himma's 6 core groups:
      CHEST, BACK, LEGS, SHOULDERS, ARMS, CORE
  - Normalizes upstream equipment into Himma's 5 core types:
      BARBELL, DUMBBELL, CABLE, MACHINE, BODYWEIGHT
  - Preserves curated manual Arabic gym names & step-by-step Arabic instructions
    from the existing seed file (~102 core gym movements).
  - Downloads 0.jpg & 1.jpg and converts them into lightweight WebP assets
    (max 480px dimension, quality 75) so the app works 100% offline via Coil.
"""

from __future__ import annotations

import argparse
import concurrent.futures
import io
import json
import sys
import urllib.request
from pathlib import Path
from typing import Any

FREE_EXERCISE_DB_URL = (
    "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
)
FREE_EXERCISE_IMG_BASE = (
    "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises"
)

MUSCLE_MAP: dict[str, str] = {
    "chest": "CHEST",
    "lats": "BACK",
    "middle back": "BACK",
    "lower back": "BACK",
    "traps": "BACK",
    "neck": "BACK",
    "quadriceps": "LEGS",
    "hamstrings": "LEGS",
    "glutes": "LEGS",
    "calves": "LEGS",
    "abductors": "LEGS",
    "adductors": "LEGS",
    "shoulders": "SHOULDERS",
    "biceps": "ARMS",
    "triceps": "ARMS",
    "forearms": "ARMS",
    "abdominals": "CORE",
}

EQUIPMENT_MAP: dict[str, str] = {
    "barbell": "BARBELL",
    "e-z curl bar": "BARBELL",
    "dumbbell": "DUMBBELL",
    "kettlebells": "DUMBBELL",
    "cable": "CABLE",
    "bands": "CABLE",
    "machine": "MACHINE",
    "body only": "BODYWEIGHT",
    "medicine ball": "BODYWEIGHT",
    "exercise ball": "BODYWEIGHT",
    "foam roll": "BODYWEIGHT",
    "other": "BODYWEIGHT",
    "": "BODYWEIGHT",
}


def normalize_muscle(raw: str | None) -> str:
    return MUSCLE_MAP.get((raw or "").strip().lower(), "CORE")


def normalize_equipment(raw: str | None) -> str:
    return EQUIPMENT_MAP.get((raw or "").strip().lower(), "BODYWEIGHT")


def infer_rest_seconds(equipment: str, primary_muscle: str) -> int:
    if equipment == "BARBELL" and primary_muscle in {"LEGS", "BACK", "CHEST"}:
        return 150 if primary_muscle == "LEGS" else 120
    if primary_muscle == "CORE":
        return 60
    if equipment in {"CABLE", "DUMBBELL"} and primary_muscle in {"ARMS", "SHOULDERS"}:
        return 60
    return 90


def fetch_upstream(url: str) -> list[dict[str, Any]]:
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "HimmaAndroidSeedSync/1.0 (+FOSS)"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def build_seed(
    upstream: list[dict[str, Any]],
    existing_seed_path: Path,
    include_extra_upstream: bool = False,
) -> list[dict[str, Any]]:
    curated_by_id: dict[str, dict[str, Any]] = {}
    curated_order: list[str] = []
    if existing_seed_path.exists():
        existing = json.loads(existing_seed_path.read_text(encoding="utf-8"))
        for entry in existing:
            curated_by_id[entry["id"]] = entry
            curated_order.append(entry["id"])

    upstream_by_id = {item["id"]: item for item in upstream if "id" in item}
    output: list[dict[str, Any]] = []

    for ex_id in curated_order:
        curated = dict(curated_by_id[ex_id])
        up = upstream_by_id.get(ex_id)
        if up:
            primary_raw = (up.get("primaryMuscles") or ["core"])[0]
            primary = curated.get("primaryMuscle") or normalize_muscle(primary_raw)
            secondary = [
                normalize_muscle(m)
                for m in (up.get("secondaryMuscles") or [])
                if normalize_muscle(m) != primary
            ]
            curated["equipment"] = normalize_equipment(up.get("equipment"))
            if not curated.get("instructionsEn") and up.get("instructions"):
                curated["instructionsEn"] = "\n".join(up["instructions"])
            if secondary and not curated.get("secondaryMuscles"):
                curated["secondaryMuscles"] = list(dict.fromkeys(secondary))
        else:
            raw_eq = str(curated.get("equipment", "BODYWEIGHT")).lower()
            curated["equipment"] = EQUIPMENT_MAP.get(raw_eq, curated.get("equipment", "BODYWEIGHT"))
            if curated["equipment"] == "OTHER":
                curated["equipment"] = "BODYWEIGHT"
        curated["localMediaKey"] = curated.get("localMediaKey") or ex_id
        curated["isCustom"] = False
        output.append(curated)

    if include_extra_upstream:
        for up in upstream:
            ex_id = up.get("id")
            if not ex_id or ex_id in curated_by_id:
                continue
            primary = normalize_muscle((up.get("primaryMuscles") or ["core"])[0])
            secondary = list(
                dict.fromkeys(
                    m for m in (normalize_muscle(x) for x in (up.get("secondaryMuscles") or []))
                    if m != primary
                )
            )
            equipment = normalize_equipment(up.get("equipment"))
            output.append(
                {
                    "id": ex_id,
                    "nameEn": up.get("name", ex_id.replace("_", " ")),
                    "nameAr": up.get("name", ex_id.replace("_", " ")),
                    "primaryMuscle": primary,
                    "secondaryMuscles": secondary,
                    "equipment": equipment,
                    "instructionsEn": "\n".join(up.get("instructions") or []),
                    "instructionsAr": "",
                    "localMediaKey": ex_id,
                    "defaultRestSeconds": infer_rest_seconds(equipment, primary),
                    "isCustom": False,
                }
            )

    return output


def download_and_convert_exercise_webp(
    ex_id: str,
    media_dir: Path,
    max_dim: int = 480,
    quality: int = 75,
) -> tuple[str, int, str | None]:
    from PIL import Image

    out_dir = media_dir / ex_id
    out_dir.mkdir(parents=True, exist_ok=True)
    written = 0
    for frame in (0, 1):
        out_path = out_dir / f"{frame}.webp"
        if out_path.exists() and out_path.stat().st_size > 0:
            written += 1
            continue
        url = f"{FREE_EXERCISE_IMG_BASE}/{ex_id}/{frame}.jpg"
        req = urllib.request.Request(url, headers={"User-Agent": "HimmaAndroidSeedSync/1.0 (+FOSS)"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw_bytes = resp.read()
        with Image.open(io.BytesIO(raw_bytes)) as img:
            rgb = img.convert("RGB")
            rgb.thumbnail((max_dim, max_dim), Image.Resampling.LANCZOS)
            rgb.save(out_path, format="WEBP", quality=quality, method=6)
        written += 1
    return ex_id, written, None


def sync_webp_images(exercises: list[dict[str, Any]], media_dir: Path, workers: int = 12) -> None:
    keys = sorted({ex["localMediaKey"] for ex in exercises if ex.get("localMediaKey")})
    failures: list[tuple[str, str]] = []
    total_files = 0

    def task(key: str) -> tuple[str, int, str | None]:
        try:
            return download_and_convert_exercise_webp(key, media_dir)
        except Exception as exc:  # noqa: BLE001
            return key, 0, str(exc)

    with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
        for ex_id, written, err in pool.map(task, keys):
            total_files += written
            if err:
                failures.append((ex_id, err))

    print(f"Synced {total_files} WebP frames under {media_dir} ({len(keys)} exercises)")
    if failures:
        for ex_id, err in failures:
            print(f"ERROR downloading {ex_id}: {err}", file=sys.stderr)
        raise SystemExit(1)


def main() -> int:
    parser = argparse.ArgumentParser(description="Sync & normalize Himma exercises_seed.json + WebP media")
    repo_root = Path(__file__).resolve().parents[1]
    parser.add_argument(
        "--seed-path",
        type=Path,
        default=repo_root / "app/src/main/assets/exercises_seed.json",
    )
    parser.add_argument(
        "--media-dir",
        type=Path,
        default=repo_root / "app/src/main/assets/exercises",
    )
    parser.add_argument(
        "--upstream-cache",
        type=Path,
        default=None,
        help="Optional local path to dist/exercises.json instead of fetching from GitHub",
    )
    parser.add_argument(
        "--include-extra",
        action="store_true",
        help="Also append additional normalized upstream exercises beyond curated Arabic set",
    )
    parser.add_argument(
        "--skip-images",
        action="store_true",
        help="Skip downloading and converting WebP exercise demonstration frames",
    )
    args = parser.parse_args()

    if args.upstream_cache and args.upstream_cache.exists():
        upstream = json.loads(args.upstream_cache.read_text(encoding="utf-8"))
    else:
        try:
            upstream = fetch_upstream(FREE_EXERCISE_DB_URL)
        except Exception as exc:  # noqa: BLE001
            print(f"Warning: could not fetch upstream ({exc}); normalizing local seed only.", file=sys.stderr)
            upstream = []

    result = build_seed(upstream, args.seed_path, include_extra_upstream=args.include_extra)
    args.seed_path.write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {len(result)} normalized exercises to {args.seed_path}")

    if not args.skip_images:
        sync_webp_images(result, args.media_dir)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
