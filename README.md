# Create: Enigma

把 **Mojang 2022 年 Java 版宣传片**里那台机器，注册成一个**能真正成型（form）的多方块结构**。

这台机器就是 Create 在**创造马达**思索里复刻并取名为 **"Mojang's Enigma"** 的那座结构——
Create 只提供了那个思索场景（`assets/create/ponder/creative_motor_mojang.nbt`），
结构布局本身来自宣传片。本模组自己重新生成了一份清洗过的结构模板，不打包 Create 的思索文件。

> **用途待定。** 当前交付的是完整的骨架：结构能放出来、能判定成型、能判定散架。
> 成型之后"做什么"还没有实现——见文末「下一步」。

---

## 1. 快速上手

```bash
# 构建（Windows）
gradlew.bat build          # 本项目未附 wrapper，见下方「构建」
```

产物：`build/libs/create_enigma-0.1.0.jar`（同时归档到 `libs-archive/`）

装好之后：

```
/place template create_enigma:mojang_enigma
```

机器会整台出现在你面前，**核心方块会在约 5 tick 后自己亮起来**（成型）。
右键核心可以随时查询当前状态；不完整时会告诉你**第一个不对的坐标和原因**。

不加坐标时以你所在位置为基准；也可以显式给坐标，并支持旋转/镜像参数：

```
/place template create_enigma:mojang_enigma ~ ~ ~
/place template create_enigma:mojang_enigma <x> <y> <z>
```

> ### ⚠️ 是 `/place template`，不是 `/place structure`
>
> 这两个是不同的子命令，很容易搞混：
>
> | 子命令 | 参数类型 | 接受什么 |
> |---|---|---|
> | `/place structure` | `Registries.STRUCTURE` | **worldgen 结构**，即 `data/<ns>/worldgen/structure/` 里注册的东西 |
> | `/place template` | 普通 `ResourceLocation` | **结构模板**，即 `data/<ns>/structure/<name>.nbt` —— 本模组就是这个 |
>
> `/place structure` 的补全列表来自结构注册表，**永远不会**列出本模组的模板。
>
> 这个坑是真实踩过的：README 最初就写成了 `/place structure`，而放置机制本身是通过
> `StructureTemplateManager` 验证的（那条路是对的），**唯独玩家真正输入的那个入口从没被执行过**。
> 现在 `theDocumentedCommandPlacesTheMachine` 这个测试会通过真实命令分发器跑这条命令，
> 并断言机器确实成型，专门堵这个洞。

### 构建说明

本项目**没有附带 gradle wrapper**——wrapper 需要联网下载 gradle 发行包，而本机到
Maven 的 IPv6 路由是黑洞的。构建请使用工作区里已备好的工具链：

```powershell
$tc = '..\..\Create Test1\.toolchain'      # 项目位于 DSH\create_test_2\create-enigma，故上溯两层
$env:JAVA_HOME      = "$tc\jdk21"
$env:GRADLE_USER_HOME = "$tc\gh"          # 共享热缓存，NeoForm 反编译结果可复用
$env:JAVA_TOOL_OPTIONS = '-Djava.net.preferIPv4Stack=true'
& "$tc\gradle\gradle-8.14.2\bin\gradle.bat" --project-dir . --no-daemon --offline build
```

首次（在新机器上）构建要反编译 Minecraft 并下载约 786 MiB 资源，十几分钟；
本机缓存已热，增量构建约 14 秒。

#### ⚠️ 工具链**故意**留在 `Create Test1` 下面，不要"顺手"搬

`.toolchain` 位于另一个工作区（`DSH\Create Test1\.toolchain`，约 1.8 GB），这是**刻意的**，
不要搬进本项目。两个原因：

1. **它不是本项目独有的。** `Create Test1\chain-reaction` 是另一个独立项目，
   目录里还装着那边的参考源码（`ref/create-src`）、脚本和日志。整个搬走等于把别人的东西也搬了。
2. **`gh`（约 1.4 GB）是 Gradle 依赖缓存，搬动有实际风险。** 缓存里存有绝对路径，
   搬完之后 Gradle 若判定缓存失效就要重新解析依赖——而本机到 Maven 的 IPv6 是黑洞的，
   **那可能是构建直接停摆，而不是慢一点。**

> 顺带澄清一个容易误判的点：构建文件里搜到的 `toolchain` 字样**绝大多数是误报**。
> `java.toolchain.languageVersion = JavaLanguageVersion.of(21)` 是 Gradle 的
> **Java 工具链特性**，和这个**目录名**只是撞词。**没有任何构建文件真的引用那个目录路径。**

#### 临时/分析文件放 `create_test_2\.work\`

本项目产出的临时文件、反编译分析工具、dump 都放在 `DSH\create_test_2\.work\`。
它**在 git 仓库之外**（仓库根是 `create_test_2\create-enigma`），所以不会被提交，
也就不需要写进 `.gitignore`。

---

## 2. 结构模板是怎么来的

原始数据是 Create 的 Ponder 捕获文件。它是**旧版**的（DataVersion 2975），
但格式与 Create 自己在 1.21.1 用的结构模板**逐字段一致**，所以可以纯转换、不需要重排。

`tools/PrepareEnigmaStructure.java` 做这几件事（跑一次即可，产物已提交）：

| 变更 | 原因 |
|---|---|
| 丢掉 y=0 的 225 格 | 那是 Ponder 的棋盘格底板，不属于机器 |
| y 整体下移 1 格 | 让机器贴地 |
| `(7,3,8)` 的箱子 → `create_enigma:enigma_core` | 见下 |
| DataVersion 2975 → 3955 | 1.21.1；不改会被 datafixer 处理一遍 |
| 清洗方块实体 NBT | 只留 7 台创造马达的 `id`/`Speed`/`ScrollValue`。其余字段（`Network`、`Source`、`Controller`、`Length`…）是 Ponder 虚拟世界里的跨方块引用，放置时会由 Create 重算，留着反而是脏数据 |

> **没有跳过任何方块。** 早先的版本额外丢掉了 `(8,3,9)` 的云杉台阶，理由是"思索场景从未揭示它"。
> **那个理由是错的**：场景第 19 步是
> `showSection(fromTo(7, 3, 9, 8, 3, 8))`，而 `fromTo` 是**闭区间长方体**，覆盖的是
> `x∈[7,8] × z∈[8,9]` 的全部四个内格——台阶在其中。丢掉它只会在小屋地面上留一个
> 1×1 的洞。现已恢复。

### ⚠️ "思索没展示" ≠ "结构里没有"

这是上一节那个错误的镜像，值得单独记下来，因为它更容易骗人。

把场景里全部 25 个选择区提取出来，跟原文件的 83 个方块逐个求交，结果是：
**有且只有一个方块从不落在任何选择区里**——

```
(6,1,3)  create:creative_motor[facing=east]
```

它是动力网络 `36558761676757`（转速 20，13 个成员）的**两台马达之一**，驱动 `x=7`
那整条皮带。动画里揭示了这条网络的另一台马达 `(5,1,8)`，**唯独漏了它**。

看起来很像是作者写选择区时漏了一格：那一簇是 `fromTo(5, 1, 2, 7, 2, 1)`，
`x∈[5,7] y∈[1,2] z∈[1,2]`——而 `(6,1,3)` 的 z=3，**差一格**。

**但"看起来像笔误"不等于它就是笔误。** 这一点已由项目作者回看
**Mojang 的宣传片**和 **Create 自己的宣传片**确认：两段视频里都明确出现了这台马达。
所以结论是：

> **马达属于这台机器，是思索场景写漏了。**

结构里保留它（现在的做法）是**忠于原物**；把它删掉才是错的。

**实践上的影响**：照思索手搭的人只会搭出 82 块，核心会拒绝成型——但**报错信息会指出
确切的坐标和期望的方块**（`(6,2,3) block is minecraft:air, expected create:creative_motor`），
所以玩家能自己补上。这也是为什么保留它不会变成"卡死人的隐藏需求"。

> 如果以后"玩家照着思索手搭"真的成为正式玩法，可以考虑给判定加一个"可选方块"的概念，
> 让这一个位置可以有可以无。当前没必要——`/place template` 会把它一起放出来。

```powershell
& "$tc\jdk21\bin\java.exe" tools\PrepareEnigmaStructure.java <ponder.nbt> src\main\resources\data\create_enigma\structure\mojang_enigma.nbt
```

结果：**83 个方块**，15×4×15。

### 与原物的刻意差异

除了"去掉底板、下沉一格、箱子换成核心"这类**机械性**变更，模板里还有**一处有意的行为改动**。
它记录在 **`tools/patches.txt`** 里——那是唯一一处本模组**不再忠实复制**原物的地方，
所以每加一条都必须写清理由。

| 位置 | 改动 | 理由 |
|---|---|---|
| `(7,3,9)` 原版漏斗 | `facing` 东 → **北** | 原物里它朝东，正对着 `(8,3,9)` 的云杉台阶。**漏斗只能往容器里送**，所以那个漏斗永远不可能工作——是个死胡同，也是这台机器"搭建者并不玩机械动力"的最明显证据之一。朝北后它指向 `(7,3,8)`，也就是本模组放核心的那一格，即机器自己的收料终点。另一个漏斗 `(8,3,8)` 朝西，本来就已经指向那里 |

结果：**两个漏斗都往核心送东西**。

#### 怎么自己加一条补丁

**改方块不需要碰任何 Java 文件**——编辑 `tools/patches.txt` 就行，用记事本即可：

```
# 一行一条，# 开头是注释
7,3,9   facing=north
```

⚠️ **坐标用原始 Ponder 坐标，不是最终结构里的坐标：**

* y 从 **1** 开始（y=0 是棋盘底板，会被丢掉）
* 最终结构里所有坐标的 y 都**减了 1**

所以原坐标 `(7,3,9)` 在最终结构里是 `(7,2,9)`。

改完在项目根目录跑一次：

```powershell
$tc = '..\..\Create Test1\.toolchain'
& "$tc\jdk21\bin\java.exe" tools\PrepareEnigmaStructure.java `
    "$tc\ref\create-src\Create-mc1.21.1-6.0.10\src\main\resources\assets\create\ponder\creative_motor_mojang.nbt" `
    src\main\resources\data\create_enigma\structure\mojang_enigma.nbt
```

**三条安全网**（都是"写错了会报错"，而不是静默生效）：

| 写错什么 | 结果 |
|---|---|
| 坐标处没有方块 | `ERROR: patches.txt:1: no block at (0,3,0), so 'facing=north' was NOT applied.` 并提示坐标系 |
| 属性名不存在 | `ERROR: minecraft:hopper has no property 'facingg'; it has [facing, enabled]` —— 直接列出实际属性 |
| 格式写错 | `ERROR: patches.txt:3: expected '<x>,<y>,<z> <property>=<value>', got: ...` —— 指出行号 |

三条都以非零码退出。这样设计是因为：**补丁没生效本身不会产生任何错误**，
模板只会少一处改动，而这种问题通常要等到进游戏仔细看才发现。

补丁文件不存在时也能跑，此时输出的是**未经改动的原物**。

### 为什么核心放在原来箱子的位置

那个格子是**机器真正的收料终点**：y=2 的安山岩漏斗朝上正对它，旁边 y=3 的原版漏斗朝西也是往它里送。
把核心放在这里，意味着玩家交互的那个方块就是机器投递的那个方块——
以后要给它挂物品栏或用途，不需要挪动任何东西。

### 为什么保留创造马达

结构里有 **7 台 `create:creative_motor`**，分布在 5 条动力网络上。创造马达生存模式拿不到，
所以这台机器本质上是**创造模式展示品**。这是刻意的选择：保持结构忠实于原物，
等用途定了再决定是否替换动力源。**如果要改成可合成动力源，会改变方块清单。**

---

## 3. 设计决策

### `assembled` 是方块状态，不是方块实体字段

原版会免费把方块状态同步给附近所有客户端，所以核心成型时改变外观**不需要本模组拥有任何自定义包**。

### 成型判定按类型白名单比属性，而不是逐属性全比

模板里很多属性**不是建造者的选择**，而是游戏推导出来的、或者会自己变的：

* Create 从周围的传动轮算出皮带的 `part`/`slope`/`facing`，从相邻窗格算出连接布尔值；
* `waterlogged` 取决于下没下雨，`powered`/`enabled` 取决于红石信号，`snowy` 取决于生物群系。

**全比会让机器因为玩家看不见也控制不了的原因拒绝成型。**
所以 `BlockStateMatcher` 比较方块本身 + 除忽略集以外的全部属性，忽略集刻意保持很短，
每一条都是"正确的建造也可能对不上"的地方。

### `UNKNOWN` 和 `UNFORMED` 是两件事

15×15 的占地最多横跨 4 个区块。**读取未加载区块里的方块会强制加载那个区块**，
所以判定从不去碰未加载的区块：读不到的位置让结论变成 `UNKNOWN` 而不是 `UNFORMED`，
调用方在 `UNKNOWN` 时**必须保持原状**。否则玩家走近机器就会把它弄散架，再搭回去全凭运气。

### 周期性重扫，而不是监听破坏事件

机器有 83 个方块，**没有一个属于本模组**——没地方记"我属于某个多方块"，
也没有哪个事件能可靠覆盖方块离开的所有方式：挖掘、爆炸、活塞、
以及 Create 的 contraption 把它整个搬走。定时重读一次形状用同一段代码覆盖全部这些情况，
而 83 次方块查询每秒一次，对一座展示品来说不值得优化掉。

### 用自己的类路径读模板，不走数据包

这样判定不受资源重载影响，也不会被覆盖了模板的数据包搞出不一致。
代价是不支持数据包覆盖模板——如果以后要做成可配置的，改 `EnigmaStructure` 一处即可。

### 不用 Registrate 的 datagen

和隔壁 `chain-reaction` 一样：blockstate / block model / item model 全部**手写**放在
`src/main/resources`。产出的 jar 里 `build/generated/resources` 是空的，
不存在生成文件与手写文件冲突的可能。物品模型是 `models/item/enigma_core.json`（3 行）。

---

## 4. 测试

```powershell
& "$tc\gradle\gradle-8.14.2\bin\gradle.bat" --project-dir . --no-daemon --offline runGameTestServer
```

`EnigmaGameTests` 有 6 个测试，在真实服务器世界里跑：

| 测试 | 断言 |
|---|---|
| `theDocumentedCommandPlacesTheMachine` | 本 README 里那条命令的 id 必须出现在 `/place template` 的补全列表里；然后用**真实命令分发器**执行那条命令，机器必须成型 |
| `completeMachineAssemblesItsCore` | 用原版结构放置把整台机器搭出来，核心必须在 40 tick 内变成 `assembled=true` |
| `removingOneBlockDisassemblesIt` | 先确认成型，再挖掉小屋地面的一块台阶，核心必须在 40 tick 内变回 `assembled=false` |
| `ponderAdvancementLoadsAndDeclaresTheAwardedCriterion` | 两个成就文件都加载成功；子成就的 parent 确实是 root；两者都声明了代码里授予用的那个准则名 |
| `wrenchRecipeLoadsAndProducesTheWrench` | 扳手配方加载成功、是有序配方、产物确实是本模组的扳手 |
| `matcherIgnoresDerivedPropertiesButNotRealChanges` | 直接检验**比较策略本身**：`waterlogged`/`snowy`/窗格连接/核心自身的 `assembled` 必须被忽略；台阶 `type`、皮带 `slope`/`part`/`casing`、以及换掉方块本身必须被判为不同 |

**当前状态：6/6 通过。**

第 1 个测试是**因为踩了坑才补上的**：README 最初把命令写成了 `/place structure`，
而那个子命令只接受 worldgen 结构，导致结构根本放不出来。放置机制本身当时是验证过的
（走的 `StructureTemplateManager`，那条路没问题），**唯独玩家真正输入的那个入口从没被执行过**。
这个测试通过真实分发器跑完整命令行，并断言机器成型——如果命令写错、id 写错或模板没被收录，
它都会失败。

第 4 个测试覆盖的是**整个模组赖以成立的那一个决策**，也是最容易搞错、
且错了只在游戏里才暴露的那个——比较太严，正确的建造会因为玩家看不见的原因被拒绝；
太松，随便一堆方块都算数。它是纯逻辑断言，不需要世界、也不会不稳定。

其中最容易漏掉的一条是**核心自身的 `assembled` 必须被忽略**：模板里存的是 `assembled=false`，
而成型后世界里是 `assembled=true`。如果这个属性参与比较，第一次判定之后的每一次判定
都会把机器重新拆掉——这是那种"能跑起来但永远不稳定"的 bug。

其余测试走的是**原版自己的结构放置**（`StructureTemplate.placeInWorld`），而不是从模板
逐个 `setBlock`——只有这条路径会创建方块实体、让 Create 连好皮带和动力网络，
也就是唯一能让"判定"受到公平检验的路径。

### 没有自动化覆盖的部分

* **未加载区块 → `UNKNOWN`** 的分支没有测试（需要构造跨区块且部分卸载的场景）。
* **客户端外观**（模型、贴图、亮度）GameTest 跑在服务端，验证不到，只能进游戏看。
* 7 台创造马达的转速是否正确恢复（16/20/20/16/16/16/−12）没有断言。

---

## 5. 已知限制

1. **生存模式不可用**——依赖 7 台创造马达。
2. **固定朝向**——不支持旋转/镜像。模板只能按原方向摆放。做旋转需要给判定加上
   `Rotation`/`Mirror` 变换，并让反馈能说明"你转错了方向"。
3. **模板不可被数据包覆盖**（见上）。
4. **不阻止破坏**——玩家可以拆掉机器，只是它会散架。没有任何"多方块保护"。
5. 结构模板里的 83 个方块**没有任何一个是本模组的**，除了核心。

---

## 6. 下一步（用途待定）

骨架已经把"结构被认出来了"这件事做成事实，剩下的是给它一个用途。几个自然的挂载点：

* `EnigmaCoreBlockEntity` —— 成型/散架时已经有 `check()` 返回的 `Result`，
  在这里加"成型时发生什么"最直接（发红石信号、给玩家成就、启动某种处理）。
* `EnigmaCoreBlock` —— 右击交互已经接好，可以换成打开 GUI。
* 核心换成 `SmartBlockEntity` —— 如果要挂 Create 的 `ValueBox`/护目镜信息/behaviour，
  需要换成 Create 的方块实体基类（当前用的是原版 `BlockEntity`，刻意少依赖）。

---

## 7. 目录结构

```
create-enigma/
├─ build.gradle / settings.gradle / gradle.properties
├─ tools/
│  ├─ PrepareEnigmaStructure.java      Ponder 捕获 -> 结构模板（跑一次）
│  └─ MakeEmptyGameTestTemplate.java   GameTest 空场地（跑一次）
└─ src/main/
   ├─ java/com/createenigma/
   │  ├─ CreateEnigma.java                       入口 + 自建 Registrate
   │  ├─ content/EnigmaCoreBlock.java            核心方块（assembled 状态、交互）
   │  ├─ content/EnigmaCoreBlockEntity.java      周期判定 + 生命周期 + 状态反馈
   │  ├─ structure/EnigmaStructure.java          模板加载（类路径 NBT）
   │  ├─ structure/BlockStateMatcher.java        属性白名单比较
   │  ├─ structure/EnigmaValidator.java          成型判定 + 放置辅助
   │  ├─ registry/                               CEBlocks / CEBlockEntities / CECreativeTabs
   │  └─ gametest/EnigmaGameTests.java           5 个 GameTest
   ├─ resources/
   │  ├─ assets/create_enigma/                   blockstate / 模型 / 中英文
   │  └─ data/create_enigma/
   │     ├─ structure/mojang_enigma.nbt          83 方块，机器本体
   │     ├─ structure/enigmagametests.empty.nbt  GameTest 场地
   │     └─ loot_table/blocks/enigma_core.json
   └─ templates/META-INF/neoforge.mods.toml
```

---

## 8. 版本

| 依赖 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | **编译目标 21.1.250**（可运行于 21.1.250+，含 21.1.255） |
| Create | 6.0.10-281 |

编译目标是**打算支持的最低版本**：向上兼容、向下不保证。缓存的 NeoForge 21.1.250
是现成的，升到 255 只会多一次依赖解析，没有兼容性收益。

---

## 9. 版本控制

### ⚠️ 所有 git 命令都必须在项目目录下运行

git 不认识"项目名"，它只认 `.git` 这个文件夹。在哪个目录敲命令，它就从那里**向上逐级查找**
`.git`：找到就算在仓库里，一路查到磁盘根目录都没有，就报：

```
fatal: not a git repository (or any of the parent directories): .git
```

所以下面这些目录**可以**（子目录也行）：

```
DSH\create_test_2\create-enigma          ✅
DSH\create_test_2\create-enigma\src      ✅
```

而这些**不行**——它们不是仓库，只是仓库上层的目录：

```
DSH\create_test_2                        ❌
DSH                                      ❌
C:\Users\赵胤棋                           ❌   ← 新开的终端默认就在这里
```

先 `cd` 进去再敲命令：

```bash
cd "C:\Users\赵胤棋\Documents\DSH\create_test_2\create-enigma"
git log --oneline
```

不确定自己在哪，先敲 `git status`：它要么报告状态，要么就是上面那句报错。

### 两个远程

| 远程 | 地址 | 状态 |
|---|---|---|
| `origin` | `https://gitee.com/Sakuracha-Kikiko/create-enigma.git` | **可用**，日常推这个 |
| `github` | `https://github.com/Sakuracha-Kikiko/create-enigma.git` | 已配置，但**本机网络推不上去**（见下） |

`origin` 已建立跟踪关系，所以推送只要：

```bash
git push
```

### 关于 GitHub

**这台机器在国内网络下连不上 GitHub**，不是配置问题：

* `git ls-remote` 偶尔能过（约 14 秒），因为它只读一小段引用列表；
* 但 `git push` 传数据时立刻 `Recv failure: Connection was reset`；
* 凭据管理器的 OAuth 登录页本身都打不开（`ERR_CONNECTION_TIMED_OUT`），认证根本走不完。

所以别把它当成"配置坏了"去排查。**开了代理之后**补推一次即可：

```bash
git push github main
```

第一次成功时凭据管理器会弹一次 GitHub 登录窗（上次没走完，凭据没存下来）。

### 日常只需要记三条

```bash
git log --oneline     # 看历史，每次提交一行
git show HEAD         # 看最近一次具体改了什么
git restore .         # 后悔药：把工作区恢复到上次提交
```

提交由 AI 助手来做。想自己确认某个版本是否已备份，用
`git status` —— 显示 "up to date with origin/main" 就是已推送。

### 两个刻意的配置

**`.gitattributes` 里 `* text=auto eol=lf`** —— 不用默认的 `* text=auto`。默认值会让 git
在 Windows 上把文件检出成 CRLF，而编辑工具写回 LF，于是工作区里会冒出一堆
"看起来是改动、其实是行尾"的假差异。锁死 LF 后行为完全确定。

**`.nbt` 被标记为 binary** —— 结构模板是 gzip 压缩数据，只要有一个字节被行尾转换动过就
彻底读不出来，而且**失败表现是"机器静静地永不成型"**，极难排查。每次改完模板都值得
用 `StructureDump` 重新解析确认一遍。

**`libs-archive/` 被忽略** —— 那是 `archiveJar` 任务存的每个版本的 jar。git 已经保留了
每个版本的源码，随时能重新构建。如果你更希望"能直接翻出旧 jar 丢进游戏"，把这个目录
从 `.gitignore` 里去掉即可，代价是二进制会永久留在历史里（每个约 35 KB）。

### 提交署名

仓库局部的占位值 `create-enigma dev <dev@localhost>`，只写进本地提交记录、不外发。
想换成自己的：

```bash
git config user.name "你的名字"
git config user.email "你的邮箱"
```

---

## 10. 致谢与出处

**这一节是事实陈述与礼节，不是版权声明。** 本模组的代码全部为本项目新写，不包含任何
第三方的代码或资源副本——因此 `LICENSE` 里只有本项目的版权人。理由见本节末尾。

### 机器布局 —— Mojang

这台机器的方块布局来自 **Mojang 于 2022 年发布的 Java 版宣传片**，用途是展示 Java 版
拥有良好的模组生态。据了解，搭建者对机械动力并不熟悉，所以成品相当抽象。

### 布局数据来源 —— Create

**Create** 在**创造马达（Creative Motor）**的思索里复刻了这台机器，取名
**"Mojang's Enigma"**（场景 id `creative_motor_mojang`）。我们是从那份思索的结构文件
`assets/create/ponder/creative_motor_mojang.nbt` 里读到布局的。

需要说明：**该文件没有被打包进本模组。** 我们只在构建期读取它一次，用
`tools/PrepareEnigmaStructure.java` 转换成自己的结构模板（并做了若干修改，见第 2 节）。
本模组分发的是转换后的产物，不是 Create 的文件。

### 贴图 —— 按名引用，未重新分发

方块模型的材质直接引用 Create 的贴图路径（`create:block/andesite_casing`、
`brass_casing`、`copper_casing`、`industrial_iron_block`），运行时由 Create 提供。
本模组不包含这些图片文件。

### 与 Mojang 和 Create 团队的关系

**本模组是第三方作品，与 Mojang 和 Create 团队均无隶属、合作或背书关系。**
两款名称仅用于说明出处。

### 为什么 LICENSE 里只有本项目的版权人

MIT 唯一的附加条件是：

> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.

它的适用对象是**该软件自身的副本**。本模组没有复制任何第三方代码或资源，
jar 内只有 `com/createenigma`、`assets/create_enigma`、`data/create_enigma` 三类内容，
所以这条要求**不被触发**——也就没有需要"保留"的第三方声明。

反过来说：把第三方写进自己的版权栏，会把"致谢"变成"权利主张"，
既可能让人误以为存在共属或背书关系，也会在日后更换许可证时留下说不清的共同持有人。
**将来若真的复制了第三方代码，正确做法是单独放 `THIRD-PARTY-NOTICES.md`，
而不是改自己的 LICENSE。**

> 以上为实务惯例层面的说明，不构成法律意见。

---

## 11. 进度（成就）

**看完「Mojang 的神秘机械」这个思索，会解锁一个进度。** 这是本模组所有神秘内容的开端。

| | |
|---|---|
| 标签页 | 「欢迎来到机械动力…？」（root，`create_enigma:root`） |
| 进度 | 「Enigma」（`create_enigma:enigma`），**刻意不做本地化** |
| 描述 | 「jang工mo械由此开始」 |
| 种类 | `task`（普通进度）。想改成 `goal`/`challenge` 就改 `enigma.json` 里的 `frame` |
| 图标 | 谜之核心（占位，随时可换） |
| 背景 | `minecraft:textures/gui/advancements/backgrounds/end.png` |

### 不要在标题/描述里写 `color`

`enigma.json` 的 title 和 description **刻意不带颜色字段**。曾经带过
`"color": "#DBA213"`（从 Create 自己的成就文件抄的写法），结果进度提示和原版成就长得不一样。

颜色的来源有两层，容易混：

* **`AdvancementType`**（`TASK` / `GOAL` / `CHALLENGE`）—— 前两个都是 `GREEN`，
  只有 `CHALLENGE` 是 `DARK_PURPLE`
* **toast 渲染**（`AdvancementToast`）—— 它画的是 **frame 标签 + 标题**，
  标签颜色写死为 `type == CHALLENGE ? 0xFFAAFF : 0xFFFF00`；
  而 `DisplayInfo.getTitle()` **原样返回组件、不注入任何颜色**

所以标题的颜色**只可能来自 JSON 里的 `color` 字段**。**想让进度和原版一致，就不要写它。**

> **已实测确认**：进度提示就是「frame 标签 + 标题」两行——`进度已达成!`（黄）+
> `Enigma`（白），聊天栏是绿色的 `取得了进度[Enigma]`。与 Create 自己的进度
> （`安山时代`）逐项一致。
>
> 这条记录留着是因为它验证了一个方法：**反编译出来的源码比肉眼看截图可靠**。
> 曾经因为看错一张截图而怀疑源码，绕了一圈才发现源码本来就是对的。

### 聊天栏通知有两个条件

原版的判定是**两个条件的与**（`PlayerAdvancements:180`）：

```java
if (display.shouldAnnounceChat() && level.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
    playerList.broadcastSystemMessage(...);
}
```

1. **`announce_to_chat`** —— 进度自己的设置。`enigma.json` 里是 `true`；
   而 **root 刻意是 `false` 且 `show_toast` 也是 `false`**，因为它是和进度同时授予的，
   否则玩家会看到两条提示（一条来自 root、一条来自进度），而 root 本身没有意义。
2. **世界规则 `announceAdvancements`** —— 默认开启。**如果玩家关过它
   （`/gamerule announceAdvancements false`），无论 `announce_to_chat` 怎么写都不会有聊天消息。**

所以"拿到了进度但聊天栏没消息"，先查这两处，而不是改代码。

值得注意的是：**在此之前，玩家看不到任何东西。** 服务端只把"对玩家可见"的进度发给客户端
（`PlayerAdvancements` 里有一套可见性判定），而未解锁、又没有已解锁子节点的 root
是不可见的。所以标签页和它的唯一一条内容会**同时出现**——揭示本身就是奖励。

### 实现链路

```
客户端 mixin          轮询 PonderScene 的播放进度
      ↓ 进度到 100%，且 getId() == create:creative_motor_mojang
发网络包              EnigmaPonderWatched（空载荷）
      ↓
服务端                授予 root + enigma 两个进度（幂等）
```

### ⚠️ 为什么不用 `setFinished`——这条最值得记

`PonderScene.setFinished(boolean)` 看起来是显而易见的钩子，**而且它是错的**：

* 它**只被 `MarkAsFinishedInstruction` 调用**（反编译确认，`PonderScene` 内部没有任何调用点）
* 而 `creativeMotorMojang` 是 `KineticsScenes` 里**唯一一个没有调用 `markAsFinished()`** 的场景
* 所以它的 `isFinished()` **永远是 `false`**

在 `setFinished` 上注入会编译通过、mixin 正常应用、**然后永远不触发**——而且不报任何错。
正确的钩子是 `getSceneProgress()`（反编译确认就是 `currentTime / totalTime`）。

### 客户端 trust

Ponder 是纯客户端的，服务端**没有任何办法**观测到玩家看没看过。所以服务端在这里选择相信客户端。

对进度（纯展示、无竞争性）这是可接受的。**但如果以后要让这个触发去解锁实际能力，
必须在服务端加真正的校验**，不能只凭这一个包。

另外载荷**刻意不带任何字段**——消息本身就是类型，所以服务端没有需要校验的客户端数据。
以后要上报更多场景，再加字段并校验，不要一上来就发一个服务端盲信的 scene id。

### 已知漏洞与未验证项

* **进度条可以拖。** `PonderUI.seekToTime(int)` 是 public 的，玩家可以直接拖到最后触发。
  对成就无所谓，但别把"进度 100%"当成"真的看完了"。
* **mixin 是否生效没有被自动化验证。** 服务端不加载 `client` 段的 mixin，客户端要有窗口，
  GameTest 两头都够不着。已做的核对是：目标方法 `tick()` 确实存在、mixin 配置合法、
  类已打进 jar。**真正确认要靠跑一次客户端。**
* **`award()` 调用本身没有测试。** 在 GameTest 里拿 `ServerPlayer` 只能靠
  `makeMockServerPlayerInLevel()`，而它会**让本整合包直接崩**——假登录会触发 Create 的
  `PlayerLoggedInEvent`，后者试图往一个没有连接的玩家发网络包并抛异常。
  所以测试停在 `award()` 的前一步：断言成就确实声明了代码要授予的那个准则名。
  这正是最可能出错的地方（准则名写错时 `award()` 只返回 `false`，不抛异常）。

### 怎么手动测

进游戏，走到创造马达的思索，翻到第二页看完。然后：

1. 应该弹出 **Enigma** 的进度提示
2. 按 L 打开进度界面 → 应该多出一个 **「欢迎来到机械动力…？」** 标签页
3. 客户端日志里应该出现：

```
Watched create:creative_motor_mojang; asking the server for the advancement
```

第 3 条是判断 mixin 有没有生效的关键证据——**如果进度没弹，先看这行有没有出现**：

| 日志里有 | 说明 |
|---|---|
| 有 | mixin 生效了，问题在服务端（看有没有 `Enigma advancements are missing` 的报错） |
| 没有 | mixin 没应用，或场景 id 对不上 |

---

## 12. 神秘扳手（Enigma Wrench）

**目前只是一个物品 + 一条配方 + 一条概要，没有任何功能。** 先做出来是为了看效果。

### 配方

框架和 Create 原版扳手一致，三处不同：

| | Create 原版扳手 | 神秘扳手 |
|---|---|---|
| 左上 / 右上 | `c:plates/gold` 金板 | **`c:plates/iron` 铁板** |
| 右下 | `create:cogwheel` 齿轮 | **`create:large_cogwheel` 大齿轮** |
| 左下 | `c:rods/wooden` 木棍 | **去掉**（整个第三行没了） |

```
GG        G = 铁板
GP        P = 大齿轮
```

### 概要

按住 Shift 显示。**两种状态下的布局都照抄 Create 自己的概要**：

```
未按 Shift                        按住 Shift
  神秘扳手                          神秘扳手
  机械动力：神秘机械                 机械动力：神秘机械
  按住 [Shift] 可查看概要            按住 [Shift] 可查看概要
                                    
                                    用于调试[神秘机械]的多功能工具…？   ← 方括号里是乱码
                                    调试啥你倒是说啊                   ← 深灰 + 删除线
```

**那条提示在两种状态下都留着，而且它前面没有空行。** 两点都是刻意的，理由都是
"别让提示框动"：

* 提示行始终是最后一行之前的固定位置，所以**按 Shift 只是往下追加，不会让整个框重排**
* 未展开时不在模组名和提示之间插空行，否则会比 Create 的物品多出一行

> 这一版之前就是这样写错的：提示只在未展开时出现，且前面多一个空行。修正是把提示
> 移出条件分支、并去掉那个前导空行。

### 乱码效果是怎么实现的

**就是 `§k`（`ChatFormatting.OBFUSCATED`）。** 原版终末之诗用的也是这个：

* 文本里本该是某个词的位置写着 `§f§k§a§b`，加载时被换成 **3–6 个字面量 `X`**
  （`WinScreen.addPoemFile:180`，随机种子写死，所以每次字数一样）
* 渲染时，凡是带混淆样式的字符**且不是空格**，就**换成一个等宽的随机字形**
  （`Font.java:411` → `FontSet.getRandomGlyph`）
* **等宽**这点很关键——乱码不会让版面抖动。而随机源是共享的，**每帧重掷**，所以会闪

所以那些 `X` 只是占位符，永远不会被真的画出来；随便什么非空格字符都行。

这里没有照抄原版那套"占位记号 + 加载时替换"，而是直接给组件加混淆样式：

```java
Component.translatable("create_enigma.enigma_wrench.subject")
        .withStyle(ChatFormatting.OBFUSCATED)
```

### 为什么不用 Create 的 `ItemDescription`

Create 有一套现成的"按住 Shift 显示概要"系统，但**它的文本在注册时就定死了**
（`ItemDescription.create` 读语言文件 → 构建成固定的组件列表）。

而这条路以后要走的方向是"**现在乱码，满足某个条件后显示真名**"——静态文本做不到。
所以概要由本模组自己在 `ItemTooltipEvent` 里构建：**每次悬停现算**。

从 Create 那里只借了**措辞**（`create.tooltip.holdForDescription` 和 `create.tooltip.keyShift`
两个语言键），这样那行提示和 Create 其它物品一字不差，但不依赖它的内部枚举。

### 这个位置比进度标题合适得多

| | 进度标题 | 物品概要 |
|---|---|---|
| 何时生成 | 数据包加载时**烘死** | **每次悬停现算** |
| 能否运行时改变 | ❌ 只能等 `/reload` | ✅ **立刻生效** |

而且**不需要任何额外联网**：客户端本来就知道玩家有哪些进度——

```java
ClientAdvancements adv = Minecraft.getInstance().getConnection().getAdvancements();
boolean revealed = adv.get(EnigmaAdvancements.ENIGMA) != null;
```

因为服务端只会把"对玩家可见"的进度发给客户端，所以这个判断就是"玩家是否已经拿到它"。

> ⚠️ 该判断的语义是"**可见**"而非严格"已完成"。服务端用 `AdvancementVisibilityEvaluator`
> 决定发什么——一个进度在自己**或某个子进度**完成时变可见。`enigma` 是叶子节点，两者等价；
> 但以后给它加了子节点，这个判断会提前变 true。

### 未验证项

* **概要的实际观感未经自动化验证。** `ItemTooltipEvent` 是纯客户端渲染路径，GameTest 够不到。
  已核对的是：物品与配方加载成功、配方产物确实是本模组的扳手（有测试断言）、
  模型父级 `create:item/wrench` 存在、`§k` 的渲染路径已从源码确认。
* **物品模型借用了 Create 的扳手模型**（`create:item/wrench`）。所以在背包里
  **它和 Create 原版扳手长得一模一样**——这是占位，随时要换。

---

## 13. 我们自己的思索：万机之首

展示**完整的机器**（含 Create 的场景从不揭示的那台马达）。

| | |
|---|---|
| 标题 | 神的作品，万机之首，厄尼格默 |
| 场景 id | `create_enigma:mojang_enigma_true` |
| 挂载物品 | `create_enigma:enigma_core`（谜之核心） |
| **所在标签** | **`create_enigma:enigma`「神秘机械」** |
| 图纸 | **引用** `create:creative_motor_mojang` |
| 语言键 | `create_enigma.ponder.mojang_enigma_true.header` |
| 标签语言键 | `create_enigma.ponder.tag.enigma` + `.description` |

### ⚠️ 标签不是可选项——没有标签的物品根本不会出现

**这是核心一直不出现在索引里的真正原因**，而且它和场景注册毫无关系。

Ponder 的界面是**按标签归档**的。`PonderTagScreen.init` 里：

```java
PonderIndex.getTagAccess().getItems(tag).stream()   // ← 物品来自"显式归档到该标签"的集合
        .map(...).filter(...).forEach(items::add);
```

**一个没有任何标签的思索物品，没有地方可以待。** 所以之前两轮排查（图纸路径、注册时序、
索引过滤器）全都在错误的方向上——**场景一直是注册成功的，缺的是归档。**

**本模组以后所有思索都放在 `create_enigma:enigma` 这一个标签下**，让整个模组读起来是
一章，而不是散落在 Create 各个分类里的零散条目。

### 和原场景唯一的差别

原场景第 3 步只揭示 `x=7` 那条皮带；我们**把 `(6,1,3)` 那台马达一起揭示**：

```java
// Create
showSection(select().fromTo(7, 1, 3, 7, 1, 8), Direction.NORTH);

// 我们
showSection(select().fromTo(7, 1, 3, 7, 1, 8)
        .add(select().position(6, 1, 3)), Direction.NORTH);
```

其余 21 步、所有 `idle(3)`、摄像机 `rotateCameraY(-90)`、结尾的 `idle(20)` —— **逐字照抄**。

**刻意没有给它单独的停顿，也没有加文字。** 它和它驱动的那条皮带在同一拍里出现。
这个场景展示的是一台**完整的机器**，不是一条**勘误**——差别是用来被对比出来的，不是被指出来的。

### 为什么不挂在创造马达下面

最初挂在创造马达上（排在 Create 的场景后面）。**那是错的**，原因见下一节：
Ponder **没有任何办法让一个场景只对部分玩家可见**，所以挂在创造马达上等于
**对所有玩家剧透**——索引页上会并排出现一台机器的"残缺版"和"完整版"。

改挂到本模组自己的方块上，才有办法加门槛。代价是**它不再紧挨着它所回答的那个场景**，
这是真实的损失，这个场景得自己站住。

### 门槛：**做不了，所以没有做**

曾经用 `IndexExclusionHelper` 加过一道门槛（"玩家看过 Mojang 的神秘机械才显示核心"）。
**它被撤掉了，因为在两个界面上表现不一致——那比不做还糟。**

原因是查证过的：

* `IndexExclusionHelper` 的谓词**只被 `PonderIndexScreen` 使用**
* 而物品实际所在的 `PonderTagScreen` **连 `exclusions` 字段都没有**，
  它的物品来自 `TagRegistryAccess.getItems(tag)`，**不受任何插件谓词约束**

所以那道门槛会：在扁平索引里藏起核心，在标签页里照常显示。**同一个物品，两个界面两个答案。**

**Ponder 没有把场景对部分玩家隐藏的受支持做法**：

* 场景在客户端启动时全局注册一次，那时连世界都没有
* `PonderScene` 上没有任何"隐藏 / 未解锁"标志
* `IndexExclusionHelper` 是**物品级**的，永远不是场景级

因此**这个场景现在对所有玩家可见**。要做成条件显示，只能 **mixin 进 Ponder 的界面**——
那是一个"决定去改别人界面"的选择，而且要接受 **Ponder 一更新就可能静默失效**的风险。

> 之前的版本还记录过一条"已确认可行"，那是错的：当时的证据链有一环是推断出来的，
> 而且用来验证的那次测试根本没测到东西（物品不在索引里，不是因为门槛，是因为没有标签）。

### 两个静默失败，都值得记

**① 图纸路径多写了一层——场景是空的，而且不报错。**

`PonderSceneRegistry.loadSchematic` 自己会拼路径：

```java
ResourceLocation file = fromNamespaceAndPath(loc.getNamespace(), "ponder/" + loc.getPath() + ".nbt");
if (resource.isEmpty()) {
    LOGGER.error(...);
    return new StructureTemplate();     // ← 空模板，不抛异常
}
```

所以正确的值是 **`create:creative_motor_mojang`**——**不带 `ponder/` 前缀、不带 `.nbt` 后缀**。
当时写成了 `create:ponder/creative_motor_mojang`，于是它去找
`create:ponder/ponder/creative_motor_mojang.nbt`，找不到，**返回一个空结构**。
结果就是：边界（底座）正常显示，机器什么都没有，游戏不崩、不报错。

**② `addStoryBoard` 没有替换语义。**

Ponder 里**无法替换或移除已注册的场景**，这是查证过的：

* `PonderSceneRegistry.addStoryBoard` 里是 `LinkedHashMultimap.put(...)` —— **追加，不覆盖**
* 注册表的全部方法只有 `clearRegistry` / `addStoryBoard` / 若干读取和 `compile`，**没有 remove**
* `IndexExclusionHelper` 只能排除**整个物品**

所以 Create 的场景原封不动。**顺序靠注册顺序**：`LinkedHashMultimap` 保插入序，
而本模组依赖 Create，所以 Create 的插件先注册、它的场景先落位。

### 命名空间

`DefaultPonderSceneRegistrationHelper` 的 namespace 字段来自插件构造时的参数，
而它来自 **`PonderPlugin.getModId()`**。所以我们的场景是 `create_enigma:...`，
语言键在 `create_enigma.ponder.*` 下，**不污染 Create 的命名空间**。

（语言键格式是从 Create 的 lang 文件反推确认的：是 `.header`，不是 `.title`。）

### 未验证项

* **画面是否正确、门槛是否生效，没有自动化验证。** Ponder 是纯客户端的，注册发生在
  客户端启动，GameTest 服务端根本不加载这些类。已核对的只有：编译通过（API 签名正确）、
  类都在 jar 里、语言键正确、引用的图纸路径存在。
* **门槛条件是占位的**（"玩家已获得 `enigma` 进度"）。按设计这一步应该更靠后，
  条件随时可以换成别的。


