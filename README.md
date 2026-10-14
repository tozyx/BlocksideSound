# Blockside Foley · 方间微响

Minecraft 客户端音效 Mod，目前开发方向是雨滴与不同材质方块接触时的声音。

当前仓库是可扩展的多加载器初始化骨架，尚未实现材质雨声或其他新增音效。

## 开发环境

- Minecraft **1.21.1**，JDK **21**。
- Gradle **8.14.3**，使用仓库附带的 Wrapper。
- Fabric **0.18.4**、NeoForge **21.1.219**。
- Architectury Loom 构建，共享代码使用 Mojang 官方映射。
- 当前无需 Architectury API 或 Fabric API 运行时依赖；需要具体 API 时再添加。

## 项目结构

```text
common/       共享 Java 代码、客户端音效实现和资源
fabric/       Fabric 客户端入口和 fabric.mod.json
neoforge/     NeoForge 客户端入口和 neoforge.mods.toml
gradle/       Gradle Wrapper
docs/         设计、开发说明
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


## 许可证与音效素材

项目许可证尚待作者决定，元数据暂标注 `All Rights Reserved`。