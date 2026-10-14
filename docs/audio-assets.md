# 雨声音频素材准备

已按用户授权从提供的 The Sound of Rain 提取资源导入 24 个短音频，覆盖七类材质与独立植物滴水。逐文件来源、原文件哈希/作者评论和裁剪记录见 [rain-audio-manifest.json](rain-audio-manifest.json)。这些是首轮试听素材，其他材质可随后增加。

## 获取素材

- 自己录制：使用滴管/喷水工具，让水落在木板、金属板、叶片和较宽的水面上。在安静环境录多个力度/间隔不同的版本；尽量避开房间回响和背景噪声。植物滴水与普通水面落雨分开录。
- [Freesound](https://freesound.org/)：搜索 `rain on wood`、`rain metal roof`、`water droplets leaves`、`rain water surface` 等。优先筛选 CC0；每个下载文件都检查其自己的来源和许可。CC BY 素材需要依照许可署名。参见 [Freesound 许可说明](https://freesound.org/help/faq/#licenses)。
- 商业音效库可以作为后续候选，但购买/免费下载前核对其是否允许随游戏/Mod 使用和分发。不能把“免费下载”当成统一授权。

不必专门找 OGG：优先保留 WAV/FLAC 原始文件，再裁剪、转换成游戏使用的单声道 Ogg Vorbis。

## 格式与处理

- 世界定位音效使用 **单声道 Ogg Vorbis**（`.ogg`），不要只修改文件后缀。参考 [Minecraft 声音与单声道要求](https://docs.neoforged.net/docs/1.21.1/resources/client/sounds/)。
- 原始录音可使用 44.1 kHz 或 48 kHz。先裁剪出清晰的短滴答或短雨滴簇，再处理明显噪声、首尾轻微淡入淡出和一致的响度；保留自然的短尾声。
- 每类首轮准备 3–5 个变体即可。素材应保持自然的轻重差异，不把所有滴声处理得同样响亮。
- 水面采用细碎溅水/柔和撞击；清晰的集中滴答用于植物滴水。
- 通过 Audacity 导出 Ogg Vorbis，见 [导出说明](https://manual.audacityteam.org/man/ogg_vorbis_export_options.html)。使用 FFmpeg 时可转换：

```sh
ffmpeg -i input.wav -ac 1 -c:a libvorbis -q:a 5 output.ogg
```

## 项目内命名

实际导入时放在 `common/src/main/resources/assets/blocksidesound/sounds/rain/`：

```text
wood_01.ogg        wood_02.ogg
metal_01.ogg       metal_02.ogg
water_01.ogg       water_02.ogg
plant_drip_01.ogg  plant_drip_02.ogg
```

素材确认后编写 `sounds.json`，每个材质事件随机选择一个变体；不同空间位置的独立事件形成混音。不要把所有变体每次同时播放。

在同一份素材清单中记录：文件名、标题/作者、来源 URL、许可及链接、下载日期、裁剪/转换等改动。署名/许可说明应随发布资源一起提供。

| 文件 | 作者/标题 | 来源 URL | 许可/证据 | 处理记录 |
|---|---|---|---|---|
| 24 个短片段，详见逐文件清单 | The Sound of Rain / JShep；原录音评论另列 | https://modrinth.com/mod/the-sound-of-rain | 发布项目声明 MIT | 较高能量短窗口、淡入淡出、Vorbis 重新编码 |

## 候选资源核对（2026-10-14）

用户提供自己 fork 的 soundofrain 项目和 [Cool-Rain 音频目录](https://github.com/Leclowndu93150/Cool-Rain/tree/master/src/main/resources/assets/coolrain/sounds)。以下保留原始候选文件统计；随后已按用户要求导入选定 24 个短片段，尚未人工试听，原始录音的单独来源说明尚未找到。

直接解析本地源资源的 Ogg 页面、Vorbis 头、评论及末页采样位置，共 98 个 Ogg Vorbis。时长是编码采样时长，不代表有效滴声的长度；原项目 sounds.json 只引用其中一部分文件。

| 文件组（rain_sounds_ 前缀） | 数量 | 声道 | 时长范围（秒） | 候选用途 |
|---|---:|---|---:|---|
| wood | 6 | 单声道 | 5.000 | 木质，试听后裁出短撞击/滴声簇 |
| metal | 10 | 单声道 | 3.021–12.510 | 金属，筛选和响度对齐 |
| water | 5 | 单声道 | 5.000 | 水面，确认是柔和溅水 |
| droplet | 4 | 单声道 | 0.348–0.500 | 植物短滴水试听候选 |
| dripping | 3 | 单声道 | 5.000 | 区分单次滴水与连续序列 |
| foliage | 14 | 单声道 | 5.000–7.616 | 植被受雨，组名不能代替听感 |
| glass | 6 | 单声道 | 6.389–7.543 | 玻璃，长环境片段需处理 |
| fabric | 6 | 单声道 | 6.547–9.670 | 柔软表面，伞面声不直接等同羊毛 |
| sand | 6 | 单声道 | 5.000 | 颗粒表面 |
| amethyst | 7 | 单声道 | 1.266–4.322 | 暂存候选，不自动新增分类 |
| lava | 3 | 单声道 | 1.248–1.500 | 后续热表面，首轮原版回退 |
| quiet | 5 | 双声道 | 1.851 | 背景候选，首轮不用于定位细节 |

其余为 12 个 rain*.ogg（双声道，2.179–2.750 秒）及 11 个雷声，雷声不属本阶段。采样率均为 44.1 kHz 或 48 kHz。

Vorbis 评论提供来源线索：fabric 作者 Ivo Vicic、标题 Rain on umbrella (Loop)；glass 作者 Tomas Herudek、标题 Rain on window ambience；部分 foliage 作者 FTUS、标题 Gentle rainfall hitting concrete。组名可能与原录音表面不同，需逐文件试听。作者标签不能代替授权。

用户补充资源是从 The Sound of Rain 发布 jar 提取后在本地工程中修改/移植。2026-10-14 已核对 [Modrinth 发布页](https://modrinth.com/mod/the-sound-of-rain) 和项目 API（project id `8GRDQ9XB`）：项目许可 `MIT`，作者/Owner `JShep`，`source_url` 为 null，没有公开源码链接。来源记录以已核对的上游发布声明为准；之前本地工程的 `mod_license=All Rights Reserved` 不能作为上游许可结论。

MIT 要求保留原版权声明和许可说明，参考 [MIT 原文](https://opensource.org/license/mit)。实际导入时随资源提供第三方说明，记录项目/作者/发布页、原文件名、提取版本（用户尚未提供原始 jar 的版本/哈希）、处理方式，以及原包中已有的版权/许可内容。不能虚构缺失的版权年份或把其他作者的音频标签改成自己的署名。本地重新构建的 `soundofrain-1.0.0.jar` 未找到 LICENSE/COPYING/NOTICE/CREDITS 条目；它不是原始发布 jar 的验证证据。

当前已确认的是发布项目 MIT 声明；已发现的 Ivo Vicic、Tomas Herudek、FTUS 等录音作者信息仍保留在素材记录中，尚未找到这些录音单独的来源说明。后续筛选可继续进行，不能在素材清单中把未知的逐文件来源填成已验证。没有源码仓库链接不妨碍记录发布页来源。

Cool-Rain 的 [sounds.json](https://github.com/Leclowndu93150/Cool-Rain/blob/master/src/main/resources/assets/coolrain/sounds.json) 包括金属、布料、玻璃、植被、水面、紫水晶、音符盒、熔岩，另有布料/玻璃/金属的 muffled 版本，可作为以后室内听感候选。[gradle.properties](https://github.com/Leclowndu93150/Cool-Rain/blob/master/gradle.properties) 声明 mod_license=MIT；本次根 LICENSE/README.md 请求返回 404，未找到逐项音频出处，尚不能确认第三方音频与代码适用同一许可。未下载远程 OGG，也未验证声道、时长或同名文件字节是否相同。

首轮来源核对后，木质、金属、水面和植物滴水各选 3–5 个变体。先试听 droplet；长片段裁出短滴声/短雨滴簇，或按时长安排稀疏播放，避免按短滴答默认间隔反复叠加 5–10 秒片段。每次随机选一个变体，在实际受雨表面播放，不同位置自然混音；植物滴水仍是独立附加层。


## 本次已导入

每组取原文件编号 1–3：wood→木质、metal→金属、glass→玻璃、foliage→植被、sand→沙砾、water→水面、fabric→柔软、droplet→植物滴水。每组独立事件三个变体；背景/雷声/熔岩/紫水晶等没有导入。全部为单声道，定位声不需要原 Mod 作为依赖。

使用可选作者工具 `scripts/prepare-rain-audio.py`：基于原录音能量选择 0.9 秒窗口（短滴水最长 0.5 秒）、4 ms 淡入、25 ms 淡出，保留采样率，重新编码 Vorbis。算法筛选未经过人工听感确认，没有把长录音自动变成物理单滴模拟。原 Vorbis 评论提取到清单，重新编码文件不保留这些评论字段。没有修改原项目资源。

正常构建无需 Python、原项目或音频工具；处理后的 OGG 已作为共享资源保存。需要重新制作本批音频时，在独立 Python 环境安装 numpy 和 soundfile==0.13.1，再运行 `python scripts/prepare-rain-audio.py --source-dir /path/to/assets/soundofrain/sounds`。该工具会重新生成这批 24 个 OGG、sounds.json 和两个逐文件清单；补新材料时直接编辑资源，不要用工具覆盖已手工补充的清单。

两个安装 jar 均包含 META-INF/licenses/the-sound-of-rain.txt（来源/署名/MIT 许可文本）及 blocksidesound-rain-audio-manifest.json（逐文件哈希与处理记录）。上游原始 jar 版本/版权年份未在提供的提取目录中记录，清单明确留空；不会编造版权声明。
