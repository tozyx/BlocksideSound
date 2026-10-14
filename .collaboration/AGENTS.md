# BlocksideSound 开发约定

适用于整个仓库，以及两台电脑上的开发会话。更新日期：2026-10-14。

- 开始工作阅读本文件、design.md、development.md、handoff.md，检查 Git 分支、工作区和实际代码。
- Minecraft 1.21.1、Java 21；使用 Gradle Wrapper，不依赖本机全局 Gradle。两平台代码改动后至少完成 Fabric、NeoForge 构建。
- 模块为 common、fabric、neoforge。共享音效逻辑、配置、客户端 Mixin 和资源放 common；加载器模块保持薄入口/适配及元数据。
- 使用 Mojang 官方映射，不直接粘贴 Yarn 名称；参考项目不作为本地 jar 或构建依赖。
- 本项目为客户端音效 Mod，客户端代码不得通过服务端入口加载；保持原版玩法和网络协议。
- Quilt 仅为 Fabric jar 的兼容性验证目标；未经实测不宣称支持，不单独维护 Quilt 实现。
- 发布版不打包或要求开发菜单依赖。开发辅助 Mod 的完整坐标写在各平台 build.gradle，enable_dev_mods 为总开关。
- 保留用户已有修改。开始交替开发前核对工作区再拉取；两台机器同时开发使用不同分支，避免覆盖。
- 区分构建/单元测试、客户端启动、雨天世界功能验收和人工试听，不以其中一种代替其他验证。
- 结束工作更新 handoff.md 的完成项、验证范围、限制和下一步；稳定决策写入 design.md，运行/交接方法写入 development.md。
- 区分用户确认的功能、已经实现的功能和候选扩展，讨论建议不自动变成实现要求。
- 按用户 2026-10-14 的新要求，.collaboration 的四份 Markdown 直接纳入 Git，随源码、资源和构建配置提交到远程。此前“私人文档不上传”的约定已被此次要求替代。
- 文档保持跨机器可用；不提交本机绝对路径、凭据、IDE 私有配置、缓存、开发世界、日志或 work 目录。
- Git 是代码与四份文档的主要交接渠道。OneDrive 脚本保留作可选备份；不要求换机前导出/导入，也不直接用旧备份覆盖 Git 最新版本。
- 如使用 sync-context.ps1，先比较同步方向与四份文档内容。不同内容默认拒绝覆盖；核对后可用 -Overwrite，原目标会备份到 work/context-backups。
