# Audio and third-party assets

The root `LICENSE` and `NOTICE` apply to BlocksideSound's original code,
build configuration and project documentation, subject to separately marked
files. They do not replace the licenses of audio or other third-party assets.
The Minecraft linking exception does not grant rights to third-party assets.

## Audio included in the mod

The 24 processed rain cues under
`common/src/main/resources/assets/blocksidesound/sounds/rain/` are derived
from audio supplied from **The Sound of Rain**, by JShep.
The upstream release project declared MIT when checked on 2026-10-14.

Preserve the original notices and the records in:

- `common/src/main/resources/META-INF/licenses/the-sound-of-rain.txt`
- `common/src/main/resources/META-INF/licenses/blocksidesound-rain-audio-manifest.json`
- `docs/audio-assets.md` and `docs/rain-audio-manifest.json`

Those records distinguish the upstream project's MIT declaration from the
original recording credits and provenance information that remains incomplete.
No new upstream copyright year, authorship or permission is asserted here.
These recordings are not claimed as original recordings by BlocksideSound.

## Reference and source audio

Files under `soundResource/` are separate source/reference material and are not
included in the mod jars by the Gradle build. Each file remains subject to its
own upstream license and notices; the project's GPL notice does not relicense it.

For the downloaded Cool-Rain files, see
`soundResource/cool-rain/README.source.md` and `SOURCE.json`. Their source is
commit `9424beff7b9329d0b48f1d434c4b717638970c51` of
<https://github.com/Leclowndu93150/Cool-Rain>.
These records establish the downloaded version and hashes; they do not assert
that every recording's original rights have been independently verified.

## Future original and AI-generated audio

No blanket CC BY, CC BY-SA or other new audio license is established by this
code-license change. For each future asset, document its creator or generation
tool/model, applicable terms, source/date and processing history. Assign a
license only for rights the contributor actually holds and is entitled to grant.
Any asset-specific permission or restriction must be recorded before release.

## Packaged notices

Release and source jars include `LICENSE`, `NOTICE`, `MINECRAFT-EXCEPTION.txt`
and this file under `META-INF/licenses/`, together with the existing audio
notices and per-file manifest. Do not remove third-party attribution when
redistributing the mod or its audio.
