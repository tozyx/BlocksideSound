# BlocksideSound · 方间微响

Minecraft 客户端音效 Mod，目前开发方向是雨滴与不同材质方块接触时的声音。

当前已接入材质雨声配置、受雨表面采样、概率调度和空间播放。七类材质及植物滴水各有三个音频变体，雨声跟随天气音量，保留原版背景；配置 GUI 和室内/背景混音后续实现。

配置、测试世界试听与补充音频方法见 [雨声开发说明](docs/rain-development.md)，素材准备见 [音频资源说明](docs/audio-assets.md)。

## 开发环境

- Minecraft **1.21.1**，JDK **21**。
- Gradle **8.14.3**，使用仓库附带的 Wrapper。
- Fabric **0.18.4**、NeoForge **21.1.219**。
- Architectury Loom 构建，共享代码使用 Mojang 官方映射。
- 发布版目前无需 Architectury API 或 Fabric API；开发环境单独加载菜单 Mod 及其依赖。

## 项目结构

```text
common/       共享 Java 代码、客户端音效实现和资源
fabric/       Fabric 客户端入口和 fabric.mod.json
neoforge/     NeoForge 客户端入口和 neoforge.mods.toml
gradle/       Gradle Wrapper
  scripts/    共享 Java 配置和平台打包逻辑
scripts/      可选文档备份和素材制作工具
```

## 构建和启动

Windows PowerShell：

```powershell
./gradlew.bat build
./gradlew.bat :fabric:runClient
./gradlew.bat :neoforge:runClient
```

macOS / Linux：

```sh
bash ./gradlew build
bash ./gradlew :fabric:runClient
bash ./gradlew :neoforge:runClient
```

两个加载器分别产生安装包：`fabric/build/libs/` 和 `neoforge/build/libs/`。
选择没有 `-dev`、`-dev-shadow` 或 `-sources` 后缀的 jar；不要将两个平台的 jar 同时安装。
`common` 的 jar 是内部构建产物，不用于安装。

## 在 IntelliJ IDEA 打开

直接打开本仓库根目录，按 Gradle 项目导入；Project SDK 和 Gradle JVM 都选择 Java 21，Gradle 使用 Wrapper。
Minecraft Development 插件可辅助代码提示、Mixin 和元数据编辑，无需再新建项目。

## 开发环境菜单 Mod

默认启动开发客户端时自动加载：

- Fabric：[Mod Menu 11.0.3](https://github.com/TerraformersMC/ModMenu/tree/v11.0.3)，配套 Fabric API 0.116.6+1.21.1。
- NeoForge：[Better ModList 21.1.4](https://modrinth.com/mod/better-modlist)，Minecraft 1.21.1 版本。

开发辅助 Mod 的完整依赖坐标直接写在各自加载器的 `build.gradle`：Fabric 使用 TerraformersMC Maven 和 `modLocalRuntime`；NeoForge 使用 Modrinth Maven 和 `localRuntime`。Better ModList 已使用 Mojang 官方名称，加载原始 jar 可保留它内置的 Placeholder API，避免 Loom 重映射丢失嵌套依赖元数据。

两者仅用于本项目开发运行，不进入编译 API、发布依赖或安装包。`gradle.properties` 只保留 `enable_dev_mods` 总开关。另一台电脑导入同一份 Gradle 配置后会自动下载，无需手工复制 jar。

菜单是客户端功能，专用服务器没有菜单界面；服务端隔离验证可使用 `:fabric:runServer` / `:neoforge:runServer`。首次服务器运行仍需自行确认 Minecraft EULA。

可关闭开发辅助 Mod，验证独立运行：

```powershell
./gradlew.bat :fabric:runClient -Penable_dev_mods=false
./gradlew.bat :neoforge:runClient -Penable_dev_mods=false
```

`common` 保持共享客户端逻辑；加载器模块只处理入口和事件适配。`gradle/scripts/java.gradle` 统一 Java 21、编码和可复现归档配置，`platform.gradle` 统一资源处理、共享代码合并及发布 jar 重映射。平台特有依赖保留在对应模块。


## 许可证与音效素材

项目代码元数据暂标注 `All Rights Reserved`。已导入的第三方雨声来自 The Sound of Rain（上游发布页声明 MIT），来源/处理记录见 [素材说明](docs/audio-assets.md) 和 [逐文件清单](docs/rain-audio-manifest.json)。署名、许可及清单也随发布 jar 打包；项目代码的许可属性不覆盖这些第三方素材的声明。


## 开发交接

协作约定、设计、开发方法和当日总结在 [.collaboration](.collaboration/handoff.md) 的四份 Markdown 中，与代码和音频一起由 Git 同步。换电脑先核对工作区，再拉取最新提交并阅读 handoff.md；OneDrive 同步脚本保留作可选备份。
