# BlocksideSound 设计记录

更新日期：2026-10-14。本文记录当前实现与稳定决策；当日验证及接续工作见 handoff.md。

## 项目与架构

| 项目 | 当前值 |
|---|---|
| 项目名 / Mod ID | BlocksideSound / blocksidesound |
| Java 包 | com.tozyx.blocksidesound |
| 中文展示名 | 暂用“方间微响” |
| 起始版本 | 0.1.0-alpha.1，Minecraft 1.21.1 |
| Java / Gradle | Java 21 / Wrapper 8.14.3 |
| 构建插件 | Architectury 3.4.164、Loom 1.13.469、Shadow 8.3.11 |
| 加载器 | Fabric 0.18.4（安装最低 0.16.0）、NeoForge 21.1.219 |
| 映射 | Mojang 官方映射 |
| 发布目标 | Fabric、NeoForge；Quilt 尚未验证 |

common 包含业务逻辑、配置、资源与客户端 Mixin；平台模块只保留入口和加载器元数据。使用 Minecraft 已有的 Gson 和声音引擎，不新增方块、物品、玩法或网络协议。当前发布版不需要 Architectury API、Fabric API；Fabric API 仅为开发菜单的运行依赖。

参考 GriefLogger 拆分共享 Java 与平台打包脚本，参考 HexMod 在各加载器模块直接写依赖坐标，不引入参考项目的业务依赖、本机路径或 jar。

## 开发依赖

- Fabric：modLocalRuntime 加载 Mod Menu 11.0.3 与 Fabric API 0.116.6+1.21.1。
- NeoForge：localRuntime 加载 Better ModList 21.1.4 原始 jar，保留内置 Placeholder API 的 Jar-in-Jar 元数据。旧菜单移植版已撤换。
- 当前 Loom 的 modLocalRuntime 重映射会移除 Better ModList 的 jarjar 元数据，曾造成菜单点击缺少 NodeParser；因此 NeoForge 使用普通 localRuntime。
- 坐标与仓库直接声明在各平台 build.gradle；gradle.properties 只保留 enable_dev_mods 总开关。开发依赖不进入发布包、编译 API 或发布必需依赖。

## 已确认的听感目标

玩家能清楚听到附近不同表面上的零散雨滴/撞击声。多种材质在各自真实位置同时参与空间混音，不只选占比最大的单一材质。

水面落雨侧重细碎溅水与柔和撞击；叶片、垂滴叶上的集中滴水是独立偶发层，不能把同样的清脆滴水作为水面通用声。音色、音量及密度需要实际试听调校。

用户认可 Mod 总音量、功能/材质音量、雨声绑定天气通道，以及以后通过 Mod 配置 GUI 和原版声音设置页入口编辑配置。GUI 和背景混音尚未实现。

## 当前实现：配置与材质

配置为 config/blocksidesound-client.json，schemaVersion=1。首次生成默认值，F3+T 重载；无效文件保留原文，继续使用最后有效设置或首次默认值。新文件缺失时从有效旧 blocksidefoley-client.json 迁移并保留旧文件，已有新文件优先。

材质为 stone、wood、metal、glass、ceramic、foliage、ground、granular、water、soft；未知与热表面为 vanilla 回退。顺序为用户 ID/tag 覆盖、内置规则、SoundType、未知回退。明确 ID 优先于 tag，同级按列表顺序；分类确定后禁用/排除不重新归类。泥土与砂砾共享原版 GRAVEL 音色，使用内置方块规则先区分土壤。

每材质支持开关、音量、概率、尝试间隔、exclude；exclude 和 overrides 支持 namespace:block 与 #namespace:tag。标签来自当前客户端/服务器同步数据，不把资源包规则当成自动创建的服务器方块 tag。植物滴水拥有独立设置与排除规则。

## 当前实现：采样与播放

- 以相机为听音中心，自行分批扫描二维 X/Z 列，复用原版高度图、降水、形状与流体查询；只访问已加载区块，补查薄植物。
- 默认水平半径 12、垂直范围 16、每 4 client tick 最多扫描 64 列。缓存分批刷新/过期；每次发声和活跃音源复核实际顶面、材质与范围。
- 调度独立于扫描，按 8×8 区域和材质分组，植物滴水另成一层；概率按有效调度尝试计，不随同一区域的方块数、帧率或扫描数直接增长。
- 默认材质概率 0.35、间隔 10 tick；植物滴水概率 0.25、间隔 40 tick。初次尝试随机错峰，同组音源不叠加。
- 全局活跃音源上限默认 8，可配置；每种最多 2，每 tick 最多启动 2 个且同种最多 1 个。较久未发声类别优先，给小面积材质机会。
- 使用真实世界坐标，WEATHER 通道和 16 格衰减，轻微随机音高。传入增益为 Mod 总音量 × 功能音量 × 材质音量 × 雨强；原版引擎应用主/天气音量和距离衰减。
- 离开范围、顶面遮挡/改变、停雨、世界切换、远距离传送、静音/关闭功能、配置重载时清理本 Mod 音源。
- 第一版完整保留原版雨声作为背景，没有原版声音拦截。禁用/排除/未知/缺失素材不增强；植物滴水可独立于 foliage 开关。
- 固定事件 rain.<类别> 已预留全部十材质及 plant_drip。资源事件不存在或为空就跳过，不占播放预算。

这些参数是开发起点，尚未经过性能基准或雨天听感调校。逐列中心点近似不等于逐滴物理模拟，复杂植物、稀疏形状及高度差仍需场景测试。

## 音频与来源

用户提供从 The Sound of Rain 发布 jar 提取的音频，发布页 https://modrinth.com/mod/the-sound-of-rain 声明 MIT，作者 JShep；没有提供源码链接。本地参考工程的 All Rights Reserved 属性不是上游许可结论。

已导入 24 个单声道 Ogg Vorbis：木质、金属、玻璃、植被、沙砾、水面、柔软及植物滴水各三变体。自动选取较高能量窗口，材质约 0.9 秒、滴水最长 0.5 秒，4 ms 淡入/25 ms 淡出并重编码。未经人工试听，不能据此认定已经得到理想孤立单滴声。

原文件名、哈希、评论、裁剪位置和处理记录保存在 docs/rain-audio-manifest.json，并随 jar 打包。许可/署名在 META-INF/licenses。原始发布 jar 版本、版权年份及原录音的单独来源未查清，记录留空，不编造声明。项目代码许可仍待选择，元数据暂为 All Rights Reserved。

Cool-Rain 仅核对了声音清单与 MIT 项目属性，其 muffled 版本可作为以后室内听感候选；没有导入其远程 OGG。

## 已确认但尚未实现

- 共享配置 GUI，Mod Menu / Better ModList 入口及原版声音设置页入口。
- 可调原版背景混音、室内遮挡/闷声效果。
- 用户将补充石质、陶质、地面等素材，届时按固定事件加入资源、字幕与来源记录。

## 候选扩展，尚未承诺实现

雨后/屋檐余滴、热表面嘶声、烟花层次、物体落地材质声、船划水/靠岸和机械互动等，不因讨论自动纳入当前阶段。

## 交接与发布

按用户当天的新要求，.collaboration 四份文档随源码和资源一起由 Git 同步。OneDrive 脚本保留作可选备份，旧目录回退兼容仍在；Git 为主要交接版本。

构建、单元测试、启动和人工试听分别验收。Quilt、专用服务器完整运行及实际雨天场景没有验证前，不作支持/完成声明。
