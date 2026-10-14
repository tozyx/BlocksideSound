# BlocksideSound 开发与跨电脑交接

更新日期：2026-10-14。四份协作文档与源码一起进入 Git；OneDrive 为可选备份。

## 新电脑准备

1. 克隆 https://github.com/tozyx/BlocksideSound.git。
2. 安装 JDK 21，确认 java -version 和 JAVA_HOME。IDEA 的项目 SDK 与 Gradle JVM 都选择 Java 21。
3. 打开仓库根目录，按 settings.gradle 导入 Gradle，使用 Wrapper；无需全局 Gradle。
4. 阅读 .collaboration 的 AGENTS.md、design.md、development.md、handoff.md，再核对分支和代码。
5. 首次构建下载 Gradle、Minecraft、映射和依赖。两台电脑可以使用不同目录，不需要参考仓库、本地 jar 或 Python 音频工具。

Gradle 用户缓存可由同机多个项目共享，但两台机器各自准备缓存。保持 IDEA 与命令行使用同一 Gradle 用户目录；不把本机 Java 路径、代理、凭据或缓存提交到仓库。

## 日常命令

Windows PowerShell，在仓库根目录：

```powershell
./gradlew.bat build --console=plain
./gradlew.bat :fabric:runClient
./gradlew.bat :neoforge:runClient
./gradlew.bat :fabric:runClient -Penable_dev_mods=false
./gradlew.bat :neoforge:runClient -Penable_dev_mods=false
```

macOS/Linux 使用 bash ./gradlew。两个安装包位于 fabric/build/libs 和 neoforge/build/libs；选择没有 dev、dev-shadow、sources 后缀的 jar。common jar 为内部产物。

两加载器任务的服务端变体为 runServer；完整专用服务器隔离尚未实测。确认 Minecraft EULA 后再运行服务器，不将客户端菜单增加为服务端入口。

## 构建组织及开发菜单

- gradle/scripts/java.gradle：Java 21、编码、源码包和可复现归档。
- gradle/scripts/platform.gradle：common 合并、Shadow、资源展开和 remap 发布包。
- 各平台 build.gradle：加载器依赖、仓库、开发菜单完整坐标，避免本机路径/参考项目依赖。
- Fabric：modLocalRuntime 的 Mod Menu 11.0.3、Fabric API 0.116.6+1.21.1。
- NeoForge：localRuntime 的 Better ModList 21.1.4，保留原 jar 的 Placeholder API Jar-in-Jar 元数据，避免当前 Loom 重映射造成 NodeParser 缺失。
- enable_dev_mods 默认 true，命令行 -Penable_dev_mods=false 关闭。菜单不进入生产编译 API、发布依赖、安装元数据或打包类。
- neoforge/gradle.properties 必须声明 loom.platform=neoforge；模块依赖仓库须含 NeoForge Maven，插件仓库与依赖仓库分别配置。
- Loom 与 Minecraft 使用 Mojang 官方映射；settings.gradle 的插件仓库为 com.mojang 提供官方 Maven 来源，解决 DataFixerUpper 的间接重定向问题。

版本：Minecraft 1.21.1、Java 21、Gradle 8.14.3、Architectury 3.4.164、Loom 1.13.469、Shadow 8.3.11、Fabric Loader 0.18.4、NeoForge 21.1.219。

## 源码和资源入口

| 职责 | 位置 |
|---|---|
| 共享客户端生命周期 | common 的 BlocksideSoundClient |
| 配置模型/读取/规则 | client/config 下 SoundConfig、ConfigStore、BlockSelector |
| 分类与表面采样 | client/rain 下 MaterialResolver、RainSampler、ColumnScanPlan |
| 事件与调度/播放 | client/rain 下 RainSound、RainPlaybackPlan、RainPlayer |
| tick / F3+T | client/mixin/MinecraftMixin 与 blocksidesound.client.mixins.json |
| 平台入口 | fabric/neoforge 下 com.tozyx.blocksidesound 对应包 |
| OGG / 清单 / 字幕 | common/src/main/resources/assets/blocksidesound |
| 第三方许可与逐文件记录 | common/src/main/resources/META-INF/licenses |
| 试听、配置与补素材 | docs/rain-development.md、docs/audio-assets.md |

配置由客户端生成在 config/blocksidesound-client.json；开发时在各模块 run/config。修改后 F3+T，保留原无效文件和最后有效配置。旧 blocksidefoley-client.json 的有效设置可迁移，旧文件保留。

## 音频编辑

正常构建直接使用仓库的 24 个处理后 OGG，不依赖原项目或 Python。补石质/陶质/地面时加入单声道音频、sounds.json 的固定 rain.<类别> 事件、字幕和来源记录；重载后自动检测，不改调度代码。

scripts/prepare-rain-audio.py 为可选素材制作工具，使用 numpy、soundfile 0.13.1。传入源 sounds 目录，可重建当前 24 个短片段及清单。它会覆盖这一批输出和 sounds.json，已有手工补充资源时不要直接覆盖；先核对差异。

## IDEA 运行配置

根项目名为 BlocksideSound。正常使用 Gradle 工具窗口的 :fabric:runClient / :neoforge:runClient；Loom 也可生成 Minecraft 应用运行配置。

本机旧 Gradle 标签及四个 Minecraft 配置的旧模块/目录引用已修正，原配置在 work/idea-name-backup-* 备份。IDEA 缓存名称时，取消当前运行配置窗口，再重新打开项目/导入 Gradle。这些属于本地 .idea 配置，不进入 Git；另一台机器重新导入自己的项目。

## 交替开发与提交

开始工作先执行：

```powershell
git status
git pull --ff-only
```

有未提交改动先核对，再拉取；分支分叉时处理原因，不强制覆盖。两台电脑同时开发使用不同分支。

结束工作更新四份文档，尤其 handoff.md 的验证范围、问题与下一步；核对变更后提交源码、资源、构建配置和 .collaboration 文档，按用户授权推送。

2026-10-14 用户明确要求四份文档直接提交远程，替代此前“私人文档仅 OneDrive”的策略。Git 是主要交接渠道，不要求新电脑先从 OneDrive 导入。

## OneDrive 可选备份

保留 scripts/sync-context.ps1，仅复制 AGENTS.md、design.md、development.md、handoff.md。默认使用 OneDrive 环境变量，目录为 BlocksideSound-context；新目录缺失时复用已有 BlocksideFoley-context，不自动移动云端目录。

```powershell
./scripts/sync-context.ps1 -Direction Export
./scripts/sync-context.ps1 -Direction Import
```

仅在需要备份或恢复时使用。不同内容默认拒绝；比较 Git 当前版本、目标内容和方向后才用 -Overwrite，原目标备份至 work/context-backups。不能用旧 OneDrive 备份直接覆盖 Git 最新文档。脚本成功只证明本机复制，云端同步以 OneDrive 客户端为准。

## 验证要求

- 代码改动：双平台 build、相关单元测试、安装包结构/依赖隔离。
- 加载或 Mixin 改动：分别启动客户端，核对初始化与声音引擎。
- 雨声行为：按 docs/rain-development.md 验收多材质位置、水面/植物滴水、音量/概率/黑名单、遮挡/移动/切换世界、暂停/F3+T。
- 试听结果与性能数据另外记录，不能用 build 或启动日志代替。
- 短时启动的 75 秒自动停止只在忽略的 work 测试脚本中，常规 runClient 没有该限制；timeout FAILED 不等于启动异常。
- CI 构建两加载器并上传安装包；本机成功不代表远程 CI 已完成。
- Quilt、无开发菜单启动、专用服务器完整运行及实际雨天试听的覆盖范围见 handoff.md。

新会话起始提示：

> 阅读 .collaboration 的四份 Markdown，核对 Git 分支、工作区和实际代码，从 handoff.md 的下一步继续。不要把历史构建/启动记录当作已经完成的雨天试听。
