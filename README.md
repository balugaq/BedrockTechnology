# BedrockTechnology (BEKT)

简介：新增机器保护修复系统：注册过的机器损坏可一键修复，另含原油机等 13 件物品、专属分类与 /bekt 管理命令。

[![Build CI](https://github.com/balugaq/BedrockTechnology/actions/workflows/build.yml/badge.svg)](https://github.com/balugaq/BedrockTechnology/actions/workflows/build.yml)

一个 [Slimefun4](https://github.com/SlimefunGuguProject/Slimefun4) 附属插件，原作者是 [CTimet](https://github.com/CTimetbukii)。BEKT 是 BedrockTechnology 的别称。

> ⚠️ 上游项目已停止维护。本仓库（balugaq fork）将其构建系统迁移到 Gradle，并把依赖升级到 Slimefun4 2025.1 / Java 21，恢复了可用性。升级适配的核心点见下方「依赖升级适配」一节。

## 它能做什么？

- **机器保护与修复系统**（核心功能）：Slimefun 机器偶尔会损坏成原版方块。玩家用**注册棒**注册机器后，损坏时可用**修复棒**修复；还有区域注册棒、修复全部棒、拥有者注册棒、拥有者修复全部棒等批量工具。服主从此不用再为玩家损坏的机器买单。
- **已注册机器的自动保护**：注册过的机器不会被破坏、燃烧、爆炸、活塞推动或 Slimefun 机器人挖掉。
- **拓展玩法**：基岩工作台（自定义配方类型）与原油生成机等机器。
- **基础物品**：资源探测芯片、挖掘核心、输送管道、水泵、原油罐装器。
- **自动注册**：放置机器时自动登记（可在配置中开关）。
- **管理命令**：`/bekt` 支持 OP 数据强制矫正、修复系统开关等子命令。

## 前置

- 服务端：Spigot / Paper 1.17 及以上（Slimefun4 2025.1 支持的版本）
- 前置插件：Slimefun4（汉化版 SlimefunGuguProject 构建）

## 安装

1. 从本仓库 [Releases](https://github.com/balugaq/BedrockTechnology/releases) 或 [鬼斩构建站](https://builds.guizhanss.net/CTimet/BedrockTechnology/master/) 下载 jar；
2. 放入服务端 `plugins/` 目录，同时确保 Slimefun4 已安装；
3. 重启服务端。配置文件 `plugins/BedrockTechnology/config.yml` 会自动生成。

## 命令

| 命令 | 权限 | 说明 |
| --- | --- | --- |
| `/bekt -cf` | OP | 强制矫正 MySQL 数据（矫正期间禁止注册/修复，严禁关服） |
| `/bekt -sft` | OP | 关闭方块修复系统 |
| `/bekt -sff` | OP | 重新开放方块修复系统 |
| `/bekt`（无参数或未知参数） | 所有人 | 输出 Wiki 链接 |

## 配置（config.yml）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `options.check-update` | `true` | 通过鬼斩构建站检查更新 |
| `options.auto-register` | `false` | 放置 Slimefun 机器时自动注册 |
| `timer.delay` / `timer.period` | `10` / `10` | 存储定时任务的初始延迟与周期（tick） |
| `mysql.enabled` | `false` | 是否用 MySQL 存储注册数据（否则用本地 `block.dat`） |
| `mysql.host` / `mysql.port` / `mysql.user` / `mysql.password` / `mysql.database` | — | MySQL 连接信息 |

## 构建

```bash
./gradlew clean build
```

产物在 `build/libs/BedrockTechnology-<version>.jar`。需要 JDK 21+（字节码目标由 `options.release` 控制）。

## 依赖升级适配（2026-11）

原项目构建于旧版 Slimefun4（RC-28，JitPack 已无法解析）。本仓库完成以下升级并做了代码适配：

| 依赖 | 原版本 | 现版本 | 说明 |
| --- | --- | --- | --- |
| Slimefun4 | `com.github.Slimefun:RC-28` | `SlimefunGuguProject:2025.1` | RC-28 在 JitPack 已无产物；2025.1 经主人确认升级 |
| GuizhanLib-api | 1.0.1 | 1.8.1 | `GuizhanBuildsUpdater` 构造器签名经 javap 验证不变 |
| spigot-api | 1.17.1 | 1.21 | 随 Slimefun4 2025.1 对齐 |
| mysql-connector-java | 8.0.30（旧坐标） | `com.mysql:mysql-connector-j` 8.4.0 | 旧坐标已废弃 |
| Druid | 1.1.9 | 1.2.23 | — |
| commons-lang | 2.x（由旧 spigot-api 传递） | `commons-lang3` 3.14.0（显式 compileOnly） | spigot-api 1.21 不再传递 |

代码适配点：

- 旧 `BlockStorage.getBlockInfoAsJson(Location)` / `setBlockInfo(Location, String, boolean)` 在 2025.1 已移除。新增 `data/BlockDataSnapshot`：用 `getLocationInfo(Location)` 全量键值 + Gson 序列化做快照，恢复时逐键 `addBlockInfo`。**注意**：这只恢复方块数据键值对，不做旧版全量恢复（含库存重建）的联动，行为可能有细微差异。
- `org.apache.commons.lang.Validate` → `org.apache.commons.lang3.Validate`。

## 反馈

- Issues：https://github.com/balugaq/BedrockTechnology/issues
- 上游 Wiki（部分内容可能过时）：https://www.yuque.com/ctimet/bedrocktechnologywiki

## 许可

[GPL-3.0](LICENSE)。转载请遵守上游 README 中的转载条款（标注原作者与原帖地址等）。
