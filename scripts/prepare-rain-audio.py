"""Prepare provisional mono rain cues from the supplied The Sound of Rain extract.

Optional authoring tool only: pip install numpy soundfile==0.13.1
Example: python scripts/prepare-rain-audio.py --source-dir /path/to/assets/soundofrain/sounds
Normal Gradle builds do not need Python or the source project.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct
import numpy as np
import soundfile as sf

GROUPS = {
    "wood": ("wood", 0.9, 0.55),
    "metal": ("metal", 0.9, 0.45),
    "glass": ("glass", 0.9, 0.40),
    "foliage": ("foliage", 0.9, 0.30),
    "granular": ("sand", 0.9, 0.45),
    "water": ("water", 0.9, 0.40),
    "soft": ("fabric", 0.9, 0.35),
    "plant_drip": ("droplet", 0.5, 0.55),
}


def comments(data):
    """Read the original Vorbis comments before the audio is re-encoded."""
    packets, pending, pos = [], b"", 0
    while pos < len(data) and len(packets) < 2:
        if data[pos:pos + 4] != b"OggS":
            raise ValueError("Invalid Ogg page")
        count = data[pos + 26]
        laces = data[pos + 27:pos + 27 + count]
        offset = pos + 27 + count
        for size in laces:
            pending += data[offset:offset + size]
            offset += size
            if size < 255:
                packets.append(pending)
                pending = b""
        pos = offset
    header = packets[1]
    size = struct.unpack_from("<I", header, 7)[0]
    offset = 11 + size
    count = struct.unpack_from("<I", header, offset)[0]
    offset += 4
    result = []
    for _ in range(count):
        size = struct.unpack_from("<I", header, offset)[0]
        offset += 4
        result.append(header[offset:offset + size].decode("utf-8", "replace"))
        offset += size
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--resources", type=Path, default=Path(__file__).resolve().parents[1] / "common/src/main/resources")
    parser.add_argument("--manifest", type=Path, default=Path(__file__).resolve().parents[1] / "docs/rain-audio-manifest.json")
    args = parser.parse_args()
    root = args.resources / "assets/blocksidesound"
    target = root / "sounds/rain"
    target.mkdir(parents=True, exist_ok=True)
    events, manifest = {}, []
    for kind, (source_kind, seconds, gain) in GROUPS.items():
        variants = []
        for index in range(1, 4):
            name = f"rain_sounds_{source_kind}_{index}.ogg"
            source = args.source_dir / name
            original = source.read_bytes()
            samples, rate = sf.read(source, dtype="float32", always_2d=True)
            if samples.shape[1] != 1:
                raise ValueError(f"Positional audio must be mono: {name}")
            samples = samples[:, 0]
            frames = min(len(samples), round(rate * seconds))
            # Pick an energetic short window rather than an arbitrary silent lead-in.
            step = max(1, round(rate * 0.1))
            starts = list(range(0, len(samples) - frames + 1, step))
            starts.append(len(samples) - frames)
            start = max(starts, key=lambda value: float(np.mean(samples[value:value + frames] ** 2)))
            cue = samples[start:start + frames].copy()
            fade_in = min(round(rate * 0.004), frames // 2)
            fade_out = min(round(rate * 0.025), frames // 2)
            cue[:fade_in] *= np.linspace(0, 1, fade_in)
            cue[-fade_out:] *= np.linspace(1, 0, fade_out)
            output_name = f"{kind}_{index:02}.ogg"
            output = target / output_name
            sf.write(output, cue, rate, format="OGG", subtype="VORBIS")
            # Decode the result too: invalid/empty output must not enter the resource set.
            check, check_rate = sf.read(output, always_2d=True)
            assert check.shape[1] == 1 and check_rate == rate and len(check) == frames
            variants.append({"name": f"blocksidesound:rain/{kind}_{index:02}",
                             "volume": gain, "attenuation_distance": 16})
            manifest.append({
                "output": f"assets/blocksidesound/sounds/rain/{output_name}",
                "original_file": name,
                "source_project": "The Sound of Rain",
                "source_author": "JShep",
                "source_url": "https://modrinth.com/mod/the-sound-of-rain",
                "declared_project_license": "MIT",
                "license_verified_date": "2026-10-14",
                "original_release_version": None,
                "original_sha256": hashlib.sha256(original).hexdigest(),
                "original_vorbis_comments": comments(original),
                "original_seconds": round(len(samples) / rate, 6),
                "crop_start_seconds": round(start / rate, 6),
                "output_seconds": round(frames / rate, 6),
                "channels": 1,
                "sample_rate": rate,
                "processing": "Energetic window; 4 ms fade in, 25 ms fade out; Vorbis re-encode. Original comments retained in this manifest.",
                "output_sha256": hashlib.sha256(output.read_bytes()).hexdigest()
            })
        events[f"rain.{kind}"] = {"subtitle": f"subtitles.blocksidesound.rain.{kind}", "sounds": variants}
    (root / "sounds.json").write_text(json.dumps(events, indent=2) + "\n", encoding="utf-8")
    manifest_text = json.dumps(manifest, indent=2, ensure_ascii=False) + "\n"
    args.manifest.write_text(manifest_text, encoding="utf-8")
    license_dir = args.resources / "META-INF/licenses"
    license_dir.mkdir(parents=True, exist_ok=True)
    (license_dir / "blocksidesound-rain-audio-manifest.json").write_text(manifest_text, encoding="utf-8")
    print(f"Prepared {len(manifest)} mono cues across {len(events)} sound events.")


if __name__ == "__main__":
    main()
