# BlocksideSound 当日总结与开发交接

日期：2026-10-14（Asia/Shanghai）。本文整理当天最终状态，替代此前按阶段追加的重复“最新完成”记录。

## 当前结论

框架、更名、材质配置/采样、现有资源的空间雨声播放已完成。双平台最终构建和 22 个测试通过；客户端短时启动已检查。实际雨天世界验收和人工试听未完成，下一步优先试听调校。

用户要求直接更新 .collaboration 四份文档，并将当日开发工作提交到远程。四份文档现随 Git 同步，旧的“忽略且只用 OneDrive”规则已改为 Git 交接、OneDrive 可选备份。

## 今天完成

### 1. 多加载器框架与开发菜单

- 保留 common / fabric / neoforge；参考 GriefLogger 拆分 Java 和平台打包脚本，参考 HexMod 在加载器模块内直接写仓库及开发依赖坐标。
- Fabric 开发环境使用 Mod Menu 11.0.3 + Fabric API 0.116.6+1.21.1；NeoForge 按用户选择使用 Better ModList 21.1.4，撤换旧菜单移植版。
- NeoForge 用 localRuntime 保留 Better ModList 的 Jar-in-Jar；此前 modLocalRuntime 重映射丢失元数据，造成 Placeholder API NodeParser 缺失的问题已定位并修正。
- enable_dev_mods 为总开关，开发依赖不进入发布包/必需依赖；发布版当前不要求 Fabric API 或 Architectury API。
- 补充 NeoForge loom.platform 和仓库配置，统一 Wrapper、Java 21 与 Mojang 映射。

### 2. 项目更名

- BlocksideFoley → BlocksideSound，Mod ID blocksidesound，包 com.tozyx.blocksidesound，共享入口 BlocksideSound / BlocksideSoundClient。
- 源码/测试目录、平台入口、Mixin、元数据、Gradle 根项目、Maven group、jar/CI 名称、文档和资源命名空间均已更新。
- 根目录及 GitHub 仓库名称由用户更新；origin 指向 https://github.com/tozyx/BlocksideSound.git。
- 新配置从有效旧 blocksidefoley-client.json 自动迁移并保留旧文件；新文件优先。旧 OneDrive-context 名称回退继续兼容。
- 用户已经改动的包迁移、暂存区和其他工作区内容均纳入最终状态，未重置丢弃。

### 3. 雨声配置与受雨表面采样

- config/blocksidesound-client.json：总/功能/材质音量、开关、概率、尝试间隔、方块/tag 排除、归类覆盖、植物滴水和音源上限。
- F3+T 重载配置/资源；错误文件保留原文，继续最后有效配置。首次启动可用默认值。
- 用户覆盖 → 内置 ID/tag → SoundType → 原版回退；明确 ID 优先 tag，禁用/排除后不重新归类；额外区分共享 GRAVEL 音色的土壤与砂砾。
- 相机附近二维列分批扫描，默认半径 12、垂直范围 16、每 4 tick 最多 64 列，只查询已加载区块；使用高度图、局部降水、形状/流体高度并补薄植物。
- 增加日志/粒子调试、缓存过期与世界/配置切换清理。

### 4. 现有音频与空间播放

- 从用户提供的 The Sound of Rain 资源中选取 24 个单声道 OGG：木质、金属、玻璃、植被、沙砾、水面、柔软、植物滴水各三变体。
- 材质自动选取约 0.9 秒的高能量窗口，滴水最长 0.5 秒；4 ms 淡入、25 ms 淡出、Vorbis 重编码。未经人工试听，不能认定是理想孤立单滴音色。
- 发布页声明 MIT、作者 JShep，无源码链接。本地参考工程 All Rights Reserved 属性不代表上游许可；保留作者评论与来源记录。
- RainPlaybackPlan 按 8×8 区域/材质独立调度、随机错峰、概率和冷却；同组不重叠、同种最多 2、每 tick 最多启动 2 个且同种 1 个、全局上限默认 8。
- 较久未发声的类别优先，给小面积材质机会。不同表面在真实世界位置发声，同事件随机选一个变体；植物滴水独立于 foliage 开关。
- WEATHER 通道、16 格衰减和轻微随机音高。增益只乘 Mod/功能/材质音量与雨强，原版引擎处理主/天气音量。
- 播放前与活跃音源复核实际受雨顶面；遮挡/换面、范围离开、传送、停雨、静音、关闭功能、世界切换和配置重载清理。
- 完整保留原版雨声背景。未知、禁用、黑名单、未定义/空音频事件不增强。stone / ceramic / ground 事件预留，后续补 OGG 与 sounds.json 即可。
- 中英文字幕、素材处理工具和逐文件清单已加入；署名、MIT 文本与清单同时随 jar 打包。

### 5. IDEA 本地配置

- 两个 Gradle runClient 标签改为 BlocksideSound，四个 Minecraft client/server 配置的旧模块/目录引用一起更新。
- 五个 XML 解析与旧名称检查通过，修改前备份到忽略的 work/idea-name-backup-*。
- IDEA 内存仍可能缓存旧配置，需要重新打开项目/导入 Gradle；这些本机 .idea 改动不进入提交。

## 验证结果与实际覆盖

| 验证项 | 结果 / 限制 |
|---|---|
| 最终双平台 build | 成功，Fabric / NeoForge 发布 jar 生成 |
| 单元测试 | 22 项：配置 7、扫描计划 2、材质解析 5、播放调度 8，全部通过 |
| 音频 | 24 个输出逐个重新解码为非空单声道；事件各三个变体，引用有效 |
| 发布包 | OGG 与清单 SHA-256 一致，署名/许可/记录存在，没有测试/JUnit/开发菜单类 |
| 最终名称 | 包、入口、元数据、资源 namespace 与 jar 一致采用新名称 |
| 开发依赖隔离 | 当天框架阶段开/关两种配置的构建和 classpath 检查通过；菜单只进开发运行，不进入发布依赖/Shadow |
| Fabric / NeoForge 启动 | 雨声阶段分别于 21:40 / 21:41 初始化、资源及声音引擎启动，没有本 Mod 资源/Mixin 错误 |
| 短时停止 | 两客户端由 work 下临时脚本在 75 秒后停止；Gradle timeout FAILED 属预设停止方式 |
| 启动后修改 | 随后完善未来材质事件/缺失素材过滤和一项测试，最终 build、22 测试与资源核验重跑；没有重新进入世界 |
| IDEA 修改 | XML 与名称检查通过；未确认当前打开的 IDEA 已刷新显示 |
| 文档总结 | 本次只整理交接与 Git 同步策略，不重复运行已完成的编译/试听检查 |

## 未完成与注意事项

- 未在实际雨天世界验收表面位置/复核、F3+T、字幕、暂停、切换世界、音量、概率、排除和空间听感；没有人工试听或性能基准。
- 首轮自动裁剪素材仍需筛选音色和调校音量/密度。水面应柔和，植物滴水应是独立偶发清脆层。
- 设置 GUI、菜单配置入口、原版声音界面入口、背景比例控制、室内闷声/低通、雨后余滴尚未实现。
- 石质、陶质、地面素材由用户后续补充。Cool-Rain 仅核对清单，没有导入远程音频。
- 原始音频发布 jar 的版本、版权年份及原录音单独出处未查清；清单留空，不编造。代码许可证仍待选择，元数据为 All Rights Reserved；第三方音频声明单独记录。
- 中心点形状/薄植物近似仍需测试复杂模组植物和高度差。没有逐滴物理模拟。
- Quilt、专用服务器完整启动、无开发菜单客户端启动未完成完整验证，不宣称已经支持。
- 素材工具会重建当前批次及 sounds.json；新增其他素材后不要直接覆盖手工补充资源。

## 接续工作顺序

1. 拉取代码和四份文档，用 JDK 21 重新导入 Gradle；阅读 docs/rain-development.md。
2. 在平原等可下雨群系建立允许命令的测试世界，执行 /weather rain；不同方向放木板、铜块、玻璃、砂、水、羊毛、树叶/垂滴叶。
3. 开启 rain.debugLogging / debugParticles，F3+T，核对 Rain scan / Rain play 的位置、分类和活跃数量。
4. 试听水面与植物滴水，测试音量/概率 0 与 1、黑名单/tag、遮挡/移动/传送/暂停/世界切换及功能关闭，记录真实结果。
5. 根据试听修改片段、概率/间隔、音量与密度；补 stone/ceramic/ground 资源、字幕和素材记录。
6. 采样/声音验收稳定后，接入共享 GUI、各菜单和原版声音设置页；背景/室内混音另行小步实现。
7. 补无开发菜单启动、专用服务器及 Quilt 兼容性验证。

## Git 与交接

- 本次提交目标：origin/main；整理前 HEAD 与远程均为 a104082（Init upload），已 fetch 确认未分叉。
- 本次包含当天框架、更名、雨声代码、测试、24 个音频、许可/清单、公共说明以及四份协作文档；本机 IDE、缓存、运行世界和工作备份按忽略规则保存。
- 提交编号、推送结果与最终工作区状态以本次交接消息及 Git 历史为准；本文不提前宣称远程推送成功。
- 另一台电脑先 git status，再在工作区可安全更新时 git pull --ff-only；四份文档随代码到达，无需先从 OneDrive 导入。
- OneDrive 脚本保留为可选备份；旧备份不能直接覆盖 Git 最新文档。

## 快速入口

- 实现：common/src/main/java/com/tozyx/blocksidesound/client/config 与 client/rain。
- 客户端接入：BlocksideSoundClient、MinecraftMixin；加载器只提供配置目录。
- 音频：common/src/main/resources/assets/blocksidesound/sounds/rain 与 sounds.json。
- 许可：common/src/main/resources/META-INF/licenses。
- 公共验收说明：docs/rain-development.md。
- 素材来源/处理：docs/audio-assets.md、docs/rain-audio-manifest.json。
