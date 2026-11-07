# AGENTS.md — BedrockTechnology 操作宪章

## TL;DR（太长了不看）

1. 本项目是 Slimefun4 附属「基岩科技 BEKT」，核心功能是机器注册/修复/保护。上游已停维护，本 fork（balugaq）负责依赖升级与维护，改动要保守。
2. 构建是 Gradle（Kotlin DSL），编译需要 JDK 21+，`options.release = 21`；改完跑 `./gradlew clean build` 验证。
3. plugin.yml 的版本号是 `${version}` 占位符，由 `processResources` 注入，**不要**写回 Maven 的 `${project.version}`。
4. 对依赖做任何升级前，先用 jar/javap 验证 API 签名，再改版本号；Slimefun4 已从 RC-28 升到 2025.1，禁止回退旧 `me.mrCookieSlime` 已删方法（见 §5 红线）。
5. 视觉/运行时行为交给主人核验，Agent 只做静态验收（编译 + grep + YAML 解析）。

## 1. 项目身份与边界

- 插件名 `BedrockTechnology`（plugin.yml `name`），主类 `io.github.ctimet.bedrocktechnology.BektMain`，`api-version: 1.17`，`depend: [Slimefun]`。
- 仓库：`balugaq/BedrockTechnology`，默认分支 `master`，上游 `CTimetbukii/BedrockTechnology`（`.github/pull.yml` 配置了上游同步）。
- 定位一句话：新增机器保护修复系统：注册过的机器损坏可一键修复，另含原油机等 13 件物品、专属分类与 /bekt 管理命令。

### 1.1 代码包地图（改代码前先定位）

```
io.github.ctimet.bedrocktechnology
├── BektMain                 # 主类：onEnable 启动顺序的唯一入口
├── core.cmd.CommandHandler  # /bekt 命令（-cf / -sft / -sff）
├── core.items
│   ├── BItems / BItemStacks # 物品注册入口 / 全部 SlimefunItemStack 定义
│   ├── group.BItemGroup     # 分类：主分类 + 基础物品/机器保护/拓展玩法 三个子分类
│   ├── repcie.BRecipeType   # 自定义配方类型（BEKT_CRAFTING_TABLE）
│   └── code
│       ├── base.BaseItemRegister     # 基础物品 5 件
│       ├── expand.ExpandItemRegister # 拓展机器：基岩工作台、原油机
│       ├── fix.MachineProtectRegister# 保护棒 6 件（注册/修复系）
│       ├── fix.*Stick                # 各类棒的右键逻辑
│       ├── expand.BCraftingTable 等  # 拓展机器实现
│       └── matrix.CenterControl      # CenterControl 机器
├── core.listener
│   ├── MachineProtectListener # 保护已注册机器（破坏/燃烧/活塞/爆炸/机器人挖掘等事件）
│   └── AutoRegisterListener   # 放置自动注册（MONITOR 优先级）
├── data
│   ├── StickData        # 注册数据门面（内存 + 持久化）
│   ├── BlockDataSnapshot# 2025.1 适配：方块数据 JSON 快照/恢复（替代旧 setBlockInfo）
│   ├── Data / FileStorage / SqlStorage / MysqlHandler
│   ├── RefuseBlockStorage / OldDataSupport
└── util
    ├── Config / Log / PlayerChat / PluginTask / Util
    └── template.AbstractGui
```

## 2. 技术栈与项目结构（事实核对）

- Java 21 编译（`options.release = 21`），运行时随服务端。
- 依赖（`gradle/libs.versions.toml`）：Slimefun4 2025.1（jitpack，compileOnly）、spigot-api 1.21（compileOnly）、GuizhanLib-api 1.8.1（shade，relocate 到 `io.github.ctimet.bedrocktechnology.guizhanlib`）、mysql-connector-j 8.4.0 与 druid 1.2.23（shade，druid relocate）、jsr305 / commons-lang3（compileOnly）。
- shade 用 `com.gradleup.shadow`，`minimize()` 开启。
- 持久化：`mysql.enabled=false` 时用 Java 序列化写 `plugins/BedrockTechnology/block.dat`（路径硬编码，见 §5 已知债务）；开启时写 MySQL 表 `bekt_new_player_data`。拒注册清单在 `plugins/BedrockTechnology/refuse.txt`。
- 无语言文件：玩家文案全部硬编码中文在源码里。

## 3. 决策树：当遇到 A 时，执行 B

### 3.1 新增/修改 Slimefun 物品或机器
- 物品显示名/lore → `BItemStacks`；注册配方 → 对应 `code/**` 的 Register 类；新分类 → `BItemGroup`（子分类需挂到 `MAIN_GROUP`）。

### 3.2 文案与配置
- 无语言文件，文案直接改源码；新增配置键要同步 `config.yml` 默认值与本文件 §2。

### 3.3 异常与日志
- 统一走 `util.Log`；命令反馈走 `PlayerChat`。不要直接 `System.out`。

### 3.4 数据存储
- 改 `StickData` / 存储实现时，注意 `readFinish` 门闩语义：未读完前禁止注册/修复。
- 方块数据快照/恢复只能走 `BlockDataSnapshot`，禁止调用已被 Slimefun4 2025.1 移除的旧 API。

### 3.5 依赖与构建
- 升级依赖：先验证坐标可解析（Maven Central / jitpack），再用 javap 验证用到的 API 签名，最后改 `libs.versions.toml` 并在注释记录原版本。
- plugin.yml 版本占位符必须是 `${version}`。

### 3.6 提交与推送
- 提交前 `./gradlew clean build`；含 `[ci skip]` 的提交不触发 Build CI。
- `gradlew` 的 git mode 必须保持 `100755`（`git ls-files -s gradlew` 检查）。

## 4. 红线（绝对禁止操作）

1. 禁止把构建迁回 Maven 或删掉 `gradle/libs.versions.toml`。
2. 禁止把 Slimefun4 降回 RC-28（JitPack 已无产物，构建必挂）。
3. 禁止调用旧版 `BlockStorage.getBlockInfoAsJson` / `setBlockInfo` / `org.apache.commons.lang`（2.x）——2025.1 / 新 classpath 下不存在。
4. 禁止在 `gradle.properties` 写本机 `org.gradle.java.home` 绝对路径。
5. 禁止改动 `.github/workflows` 的分支名（master）与 `chmod +x gradlew` 步骤。
6. 禁止把 `src/main/resources/block.dat`、`refuse.txt` 当作可删除的冗余文件（是默认数据模板）。
7. 禁止绕过 `StickData.readFinish` 门闩直接写存储。
8. Agent 不得启动浏览器/截图工具做视觉验证。

## 5. 质量规范与已知债务

- 已知债务（改动相关代码时顺手评估是否修复，但不要顺手大重构）：
  - `ListenerRegister.init()` 把 `AutoRegisterListener` 无条件注册了一次，又在 `options.auto-register=true` 时再注册一次——重复注册是上游遗留，修复前先确认行为影响。
  - `FileStorage` 硬编码 `plugins/BedrockTechnology/` 路径。
  - `BlockDataSnapshot` 只恢复键值对，不做旧版全量恢复（含库存），与 RC-28 行为有细微差异。
  - `CommandHandler` 的非 OP 分支只是输出 Wiki 链接（上游注释称命令系统在重构中）。

## 6. 常用 Gradle 命令速查表

```bash
./gradlew clean build          # 完整构建（含 shadowJar）
./gradlew compileJava          # 只编译
./gradlew shadowJar            # 只出插件 jar
./gradlew runServer            # 本地起测试服（需配置 run 目录）
```

## 7. Git 工作流

- 分支 `master`；上游同步由 `.github/pull.yml`（Pull app）负责，不要手工 merge upstream 后强推。
- CI：`build.yml`（push/PR，JDK 21）、`release.yml`（gradle.properties 版本变化时自动发 Release）、`test-compatibility.yml`（Paper 1.20.1 / 26.2 启动测试，依赖从鬼斩资源站下载 Slimefun4）。

## 8. 文档更新契约

- 改动启动顺序、包结构、配置键、命令、依赖版本时，必须同步更新本文件与 `README.md`，且与代码保持一致；README 的定位一句话不得与实际功能矛盾。
