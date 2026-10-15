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

### 支持水平旋转（四个朝向都认）

机器是**15×4×15，水平方向正方形**，所以四个朝向在尺寸上都成立。玩家绕一圈发现只有另一个
朝向放得下，是很正常的事——所以判定会**逐个朝向试**，任一完整匹配就算成型。

**这主要是为了避免误导，不只是为了方便。** 不支持的后果不是"没建成"，而是核心报出
一个**和玩家实际搭建完全对不上的坐标**——那条精心设计的诊断信息会变成噪音，
而且恰好是在最需要它的时候。

三处改动：

| | |
|---|---|
| `EnigmaStructure.offsetFor(offset, rotation)` | 把模板偏移转到旋转后的位置 |
| `EnigmaValidator.validate` | 四个朝向依次试，先试 `NONE` |
| `EnigmaValidator.place(level, core, rotation)` | 放置时带上旋转 |

**旋转是绕核心，不是绕模板角。** 核心是识别这台机器的唯一锚点，所以它必须**转的时候不动**。
绕模板角转会把核心挪走——而核心在 `(7,2,8)`（x 居中、**z 偏 1**），每个朝向都会把它送到
不同的地方，锚点就没了。

**⚠️ 这里有一个必须成对出现的设置：**

```java
// 放置：原版默认绕 BlockPos.ZERO 转，必须显式设支点
new StructurePlaceSettings().setRotation(rotation)
                            .setRotationPivot(structure.coreOffset());

// 判定：自己的数学，绕核心
origin.offset(structure.offsetFor(entry.offset(), rotation));
```

**这两边必须完全一致。** 核心是旋转的不动点，所以 `origin = corePos - coreOffset`
这个式子对四个朝向都成立，不需要额外的支点运算。

**符号约定**（从原版 `StructureTemplate.transform` 反推）：

```
CLOCKWISE_90         (x, z) → (-z,  x)
CLOCKWISE_180        (x, z) → (-x, -z)
COUNTERCLOCKWISE_90  (x, z) → ( z, -x)
```

**这个符号搞反了不会报错**——转过的机器只是永远认不出来。所以有三个测试
（90°/180°/270°）**通过原版放置、再通过我们自己的数学判定**，逼两边保持一致。

> 那个 bug 真的发生过一次，而且是被测试抓住的：漏了 `setRotationPivot` 时，
> 核心被转到了别处，测试报的是
> `Cannot get property BooleanProperty{name=assembled} as it does not exist in Block{minecraft:air}`
> ——**核心位置上什么都没有了**。如果只靠肉眼验证，这个会很晚才发现。

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

### 推送被拒怎么办（`! [rejected] ... (fetch first)`）

这个报错的意思是：**远端有你本地没有的提交**，所以 Git 拒绝覆盖它。**两个远程各自被推过、
历史分叉了**的时候就会这样——本项目真的发生过一次：GitHub 上有一个直接在网页端做的
LICENSE 提交，而本地/Gitee 那条线上后来把那行又删掉了。

**处理顺序（不要跳步）：**

```bash
git fetch github                      # 1. 先看清楚远端到底有什么
git log --oneline github/main         # 2. 看看那几个提交是什么
git merge-base main github/main       # 3. 有没有共同祖先？没有就不能直接合并
git pull --rebase github main         # 4. 把本地提交重放到远端提交之上
git push github main                  # 5. 这时是快进推送，不需要 force
```

### ⚠️ 不要用 `git push --force`

`--force` 会**直接覆盖远端**，把那边你还没有的提交永久删掉。报错信息里虽然会提到
`--force`，但那是给"确定远端的东西不要了"的场景准备的。

**如果确实必须重写历史**（比如 rebase 之后远端对不上了），用
**`--force-with-lease`**，而且必须先证明远端没有会丢的东西：

```bash
git fetch origin                                   # 让 lease 基于最新状态
git cherry main origin/main                        # 按"补丁内容"比对，不看哈希
git diff origin/main main --stat                   # 文件内容真的没差别？
git push --force-with-lease origin main
```

* `git cherry` 输出里以 `+` 开头的提交，才是"远端有、本地没有（按内容比）"的
* **`--force-with-lease` 与 `--force` 不同**：远端若有你没抓到的变动，它会**自动拒绝**，
  而不是闷头覆盖

> 本项目实战过一次：rebase 之后 `git cherry` 显示缺失 0 个、`git diff` 为空，
> 确认内容一字未变，才用 `--force-with-lease` 对齐的 Gitee。
> 动手前还打了个 `git tag backup-before-rebase` 留退路。

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

**看完任意思索会解锁第一个进度；看完「Mojang 的神秘机械」再解锁第二个。**
这是本模组所有神秘内容的开端。

| | 标签页 | 俺寻思之力 | Enigma |
|---|---|---|---|
| id | `create_enigma:root` | `create_enigma:ponder_watched` | `create_enigma:enigma` |
| 触发 | 不可触发（只是容器） | **看完任意思索** | 看完 `create:creative_motor_mojang` |
| 图标 | 谜之核心 | **`create:goggles`**（工程师护目镜） | 谜之核心 |
| 描述 | ……你确定这是机械动力吗？ | jang工mo械由此开始 | 神秘学…它赋予世间某种秩序以成就奇迹，令人感知它的存在，并为之惊叹不已 |
| 种类 | — | `task` | `task` |
| 背景 | `minecraft:.../backgrounds/end.png` | — | — |

### ⚠️ 三个进度是**一条链**，不是三个兄弟

```
root ──► ponder_watched ──► enigma
```

**这不是解锁顺序**——进度由代码授予，父节点从不限制任何东西。**这是为了排版。**

`TreeNodePosition` 排布子节点时直接遍历 `AdvancementNode.children()`，而那是
**`ReferenceOpenHashSet`**——按**对象身份哈希**的无序集合。所以：

> **同一层的进度谁在前谁在后，数据包控制不了，而且每次启动都可能不一样。**

把 `ponder_watched` 和 `enigma` 都挂在 root 下面，两者会**随机换位**；连成一条链则只有一种排法。
`EnigmaGameTests` 里有断言盯着这两条父子链接——链接断了进度照样发放，只是显示在错的地方，
没有测试根本看不出来。

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

#### ⚠️ 但"注册标签"和"把物品放进去"是两件事

**这是第二个坑，而且更隐蔽。** 下面两种写法**看起来**都能把物品归入标签，**其实都不能**：

```java
// ✗ 只是设图标
helper.registerTag(TAG).addToIndex().item(CORE, true, false).title(...).description(...).register();

// ✗ 那个可变参数是"高亮标签"，给界面用的，不进注册表
helper.addStoryBoard(CORE, SCHEMATIC, Board::build, TAG);
```

**真正归档的只有 `addTagToComponent(component, tag)`**，用建造器写就是：

```java
helper.addToTag(TAG).add(CORE);          // ✓ 这一句才是归档
```

原因在 `PonderTagRegistry`：标签的物品来自 `componentTagMap`，而这个映射**只有**
`addTagToComponent` 会写。判断依据是两条字节码事实：

* `TagBuilder.item(ItemLike, boolean, boolean)` 只有两个 `putfield`（`itemIcon` / `mainItem`），
  **没有任何写映射的动作**
* `addTagToComponent` 的调用者列表里**没有 `PonderSceneRegistry`**——所以场景注册时的标签参数
  帮不上忙

Create 自己的写法也是 `HELPER.addToTag(TAG).add(...)`。

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

### 门槛：**能做，靠的是"重新注册"**

**曾经记录过"做不了"，那是错的。** 正确的机制是 **`PonderIndex.reload()`**，公开 API：

```java
public static void reload() {
    LOCALIZATION.clearShared();
    SCENES.clearRegistry();      // 清空 multimap，并把 allowRegistration 置回 true
    TAGS.clearRegistry();
    registerAll();               // 重跑每个插件的 registerScenes / registerTags
    gatherSharedText();
}
```

而 `PonderSceneRegistry.clearRegistry` 的字节码是：

```java
this.scenes.clear();
this.allowRegistration = true;   // ← 注册阶段被重新打开
```

**所以"这个场景该不该存在"可以在每次重载时重新回答一次**，读的是活的客户端状态：

```java
@Override
public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
    if (!unlocked()) return;     // 不注册 = 场景不存在
    helper.addStoryBoard(...);
}
```

**为什么以前以为做不到**：只看到了 `addStoryBoard` 在注册阶段结束后会抛
`Registration Phase has already ended!`，却没注意到 `clearRegistry()` 会把那个开关拧回去，
于是"注册只能发生一次"就成了一个错误的前提。

> **这个机制是从 Ponderer 这个模组学来的**（作者 nododiiiii，MIT）。
> 它的 `PondererNeoForgeClient` 里写着：
> `PonderIndex.addPlugin(new DynamicPonderPlugin()); // Do NOT call PonderIndex.reload() here.`

**时机由 `EnigmaPonderUnlockWatcher` 决定**：

* 条件判断是**轮询**的。客户端本来就持有玩家的进度列表（服务端发的），所以不需要发包、
  也不需要等事件——只是没有"进度刚被加进客户端副本"的事件，于是每 tick 比一次布尔值。
* **重载会推迟到没有界面打开时。** `PonderIndex.reload()` 重建的是**全游戏**的思索，
  不只是本模组的；而条件恰恰是在玩家**正看着思索**的时候翻转的，所以必须等界面关掉。
  等一等没有代价——索引只在被打开时才被读取。

**标签也一起被门禁。** 目前本模组只有这一个场景，如果标签无条件存在，那"这里有个章节但里面
什么都没有"本身就是个提示。等以后有该常驻显示的场景时，标签的注册会移出门禁。

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

---

## 14. 伪装创造马达（Disguised Creative Motor）

**用途：让玩家能在生存模式里搭起那台机器。** 原机器需要 **7 台创造马达**，
而创造马达是创造模式专属——所以生存玩家永远搭不出来。这是一个**自举问题**。

| | |
|---|---|
| 行为 | **和创造马达完全一样**：转速可调（±256）、方向可调、面板可用、会转 |
| 应力 | **1 SU/RPM**（创造马达是 16384） |
| 外观 | **和创造马达逐像素一样**——方块状态直接指向 Create 自己的模型 |
| 概要 | 按住 Shift：「俺寻思这是创造马达」 |
| 配方 | 安山岩机壳 + 品红色染料（**占位，见下**） |

### 为什么是"镜像"而不是"继承"

`CreativeMotorBlock` 里写着 `implements IBE<CreativeMotorBlockEntity>`，而
`getBlockEntityClass()` 返回 `Class<CreativeMotorBlockEntity>`。**子类没法把它收窄成自己的
方块实体类型**——泛型不协变——所以继承它只能保留 Create 的方块实体。抄那六个小覆盖更省事。

方块实体那一层是**真的继承**（`extends CreativeMotorBlockEntity`），所以转速面板、
网络行为、渲染全部白拿。

### 两处 Create 写死的地方，都必须绕

**① `getGeneratedSpeed()` 写死了方块类型：**

```java
// Create 的实现
public float getGeneratedSpeed() {
    if (!AllBlocks.CREATIVE_MOTOR.has(getBlockState()))
        return 0;                       // ← 伪装马达会被判成永远不转
    ...
}
```

**② `CStress.setCapacity` 拒绝别的模组：**

```java
private static void assertFromCreate(BlockBuilder<?, ?> builder) {
    if (!builder.getOwner().getModid().equals(Create.ID))
        throw new IllegalStateException("Non-Create blocks cannot be added to Create's config.");
}
```

所以**应力不能走 Create 的配置系统**，改在方块实体里覆盖
`calculateAddedStressCapacity()`——那是 `KineticBlockEntity` 的方法，可以正常覆盖。

> 覆盖时**必须同时写 `lastCapacityProvided`**：基类也写它，而且它会被写进网络标签、
> 推给动力网络。只 `return` 不赋值会让两边不一致。

### 一个必须用对属性实例的细节

`CreativeMotorBlockEntity` 里那个转速面板读的是
`state.getValue(CreativeMotorBlock.FACING)`。所以我们的方块**必须用同一个属性实例**，
否则会抛异常。好在 `DirectionalKineticBlock.FACING` 就是
`BlockStateProperties.FACING` 本身——单例，所以两边拿到的是同一个对象。

### 配方是**占位的**

「安山岩机壳 + 品红色染料」是我替你想的（"把机壳刷成创造马达的颜色"）。
**没有和你确认过**，改起来就是一个 JSON。它便宜是故意的——要 7 个，
而且这一步的作用是**解锁搭建**，不该变成第二道资源门槛。

### 未验证项

* **渲染是纯客户端的，没有自动化验证。** `.renderer(...)` 和 `.visual(...)` 服务端不加载。
  已核对的是三个借用的类（`CreativeMotorRenderer`、`OrientedRotatingVisual`、
  `AllPartialModels.SHAFT_HALF`）都是 public、签名匹配、编译通过。
* **判定已经接受它了** ✓ —— 见下一节（"不挑食"就是这件事的技术含义）。
  但它**还没有被大规模实测**：目前只有判定层的自动化测试，没有真人搭过一遍。

---

## 15. 一代：玩家真正能搭出来的那台

**一代 = 完整机器的三个改动**，而且**是在内存里推导出来的，不额外发 NBT**：

| | 锚点 | 马达 | 块数 |
|---|---|---|---|
| **PRIMAL** 玩家搭的（扳手读它） | **箱子** | 6 × 伪装 | 82 |
| **ASSEMBLED** 核心校验自己（核心读它） | **核心** | 6（任意） | 82 |
| **COMPLETE** 原物 | 核心 | 7 | 83 |

> **⚠️ `ASSEMBLED` 必须单独存在，这是个概念错误换来的。**
>
> 第一版只有 PRIMAL 和 COMPLETE 两个状态 ✗ 而核心**自己就站在一代的锚点上**——
> 一代期待那里是个**箱子** ✗ 所以核心**永远无法通过自己的校验** ✗
>
> 表现是六个测试同时红：`validator says: FORMED` 但核心**没有成型** ✓
> ——判定说没问题、状态却没变，因为判定的是"箱子版本"，而现场是"核心版本"。

**三个状态都是从同一份模板推导的**，所以它们除了这三处差异之外**不可能不一致** ✓

> **为什么推导而不是再发一份 NBT**：两份文件会漂移——一份重新生成了、另一份忘了——
> 而失败的样子是"能这样搭、却那样判定"。推导出来的两份**不可能不一致**。

**"不含那台有争议的马达"是刻意的** ✓ ——玩家只能搭出他们被展示过的东西，而思索展示的是六台。
第七台要等到他们**有理由怀疑那个展示**之后才出现 ✓

### 判定上的"不挑食"

`BlockStateMatcher` 里有一张替代表：**`disguised_motor` 和 `creative_motor` 互相等价** ✓

**这就是"它现在并不挑食"的技术含义** ——而且它是**双向**的，因为两份模板各记一种：
一代记伪装、完整机器记真品，而玩家两种都可能放在那个位置上 ✓

**只有方块被替换，属性照旧比** ✓ ——两种马达都带 `facing`，所以**朝向错了照样拒绝** ✓

### 扳手右键箱子：五档反馈

| 已有 | 话 |
|---|---|
| 0~5 | 这是一个箱子 |
| 6~20 | 这是一个箱子…？ |
| 21~60 | 它正渴求着更多… |
| 60+ 但不完整 | 神秘的力量正在汇聚… |
| **只差马达** | **它已经饥饿了太久，它现在并不挑食** |

**⚠️ 顺序是硬要求：最后一档必须最先问。**

因为"只差马达"必然也满足"60+"（一代 82 块，只差 6 台马达 = 已有 76 块）。
顺序写反了那句话**永远不会出现**，而且**不会报错** ——你只会觉得"这句怎么没见过" ✓

**按住 Shift 再右键** = 换成精确诊断（复用核心那套 `不完整 - (x,y,z) …`）✓
氛围归普通右键，**卡住的时候有地方问** ✓

### 右键箱子不会打不开 —— **上一版这里是错的**

第一版用了 `Item.useOn`，理由是"箱子响应的是 `useWithoutItem` 而不是 `useItemOn`，
所以手拿扳手时方块自己让开"。

**那个理由是错的** ✗ ——实机结果是**箱子照开，提示一次都没出现** ✓

真正的原因是原版的顺序：

```java
// ServerPlayerGameMode.useItemOn
stack.onItemUseFirst(context)          // ← 先问物品
if (result != InteractionResult.PASS) return result;

blockstate.useItemOn(...)              // ← 箱子在这里被打开
...
stack.useOn(context)                   // ← 而 useOn 在这里，已经晚了
```

**`useOn` 在方块之后** ✗ 所以箱子先开 ✓

修法是改用 **`onItemUseFirst`** ✓ ——它在方块之前跑，返回非 `PASS` 就**消费掉这次点击** ✓
**依然不需要取消任何东西**，也不需要事件订阅 ✓ 只是钩子选对了 ✓

> 教训：**"这个方法应该不会被调用"不能靠推理** ——我当时是从"箱子不覆写 `useItemOn`"推出的，
> 推理链本身没错，但**漏看了顺序**。而顺序在原版代码里明明白白写着。**

### 箱子不再要求朝向

`minecraft:chest` 的忽略属性从 `{type}` 扩到 **`{type, facing}`** ✓
`facing` 只决定盖子往哪边开，是纯装饰 ✓ 要求玩家把箱子摆成正北毫无道理 ✓

### 完整时再右键 → 箱子换成核心

**箱子里的东西会先掉一地** ✓ —— 箱子是容器、机器又是围着它搭的，玩家一定会往里放东西；
悄悄删掉是那种**没有解释、也没法撤销**的损失。

**然后发生的事情是设计好的：**

> 完整模板记着**七台**马达，而一代只有**六台** —— 所以刚放下去的核心**立刻报告自己不完整**，
> 并**报出那台马达的坐标** ✓
>
> **那台思索从没展示过的马达，由机器自己说出来。** 用它一直以来的那个声音。

### 三个层次

**成型是二值的，层次是另一回事。** 三个层次都是**能运转的机器**，差别只在**是什么在驱动它**：

| 层 | 条件 | 说明 |
|---|---|---|
| **`LOW`** | 6 台马达，**至少一台是伪装** | 生存玩家今天就能搭出来的 |
| **`MIDDLE`** | **6 台真品**，第七个位置还空着 | |
| **`HIGH`** | **7 台全是真品** | 它被造出来时的样子 |

**基础机器只要 6 台，而且接受伪装** ✓ ——这一点是**整个进程能启动的前提**：
机器产出的正是**真创造马达**，如果它反过来要求 7 台真品，那就是死锁 ✗

**七个马达的位置不是写死的** ✓ ——它们**从完整模板里读出来**（凡是 `isMotor` 的条目）✓
模板以后改了，层次判定会跟着改，而不会悄悄错掉 ✓

**判定方式**：四个朝向都试，**马达最多的那个朝向胜出** ✓ 所以转过的机器按"里面装了什么"分类，
而不是按校验器碰巧往哪看 ✓ 每轮只读七个位置，够便宜 ✓

**未加载的区块一律读作"马达不在"** ✓ ——所以层次只会**低估，不会凭空造出** ✓

**每层的功能还没做**（`"用途待定"`的老问题）✓ 现在只做到**识别 + 报告** ✓
核心右键会说自己在哪一层 ✓ ——这就是以后挂内容的地方 ✓

* **11 个测试全部通过** ✓ 其中两个是一代的：完整读出（82/82）、以及"只差马达"被单独识别 ✓
* 那台马达的位置 `(6,0,3)` 是**从成品模板的方块实体数据里核出来的**，不是推的 ✓

### 未验证项

* **扳手的右键交互没有自动化测试。** 在 GameTest 里拿玩家只能靠 `makeMockServerPlayerInLevel()`，
  而它会让本整合包直接崩（见 §11）。判定层测到了，**"右键→提示→替换"这一串没有** ✓
* **没人真正搭过一遍。** 82 块手搭到底什么体感，只有搭过才知道 —— 而那正是决定后面所有数字的东西。
* **一代的图纸怎么让玩家知道** —— 现在有了：**蓝图**（见下一节）✓

---

## 16. 蓝图：把"搭"这件事交还给 Create

**实测结论：82 块手搭是折磨，不是挑战** ——它没有内在逻辑，玩家也没有直观的参照。
所以这一节做的事是：**让玩家能捡到一张蓝图，用 Create 自己的蓝图大炮把它打出来。**

### 为什么可行：蓝图只是一个"文件名 + 所有者"

`create:schematic` 身上只有两个数据组件 ✓ ——**结构本身不在物品里** ✓：

```java
// SchematicItem.loadSchematic
if (!level.isClientSide()) {
    dir  = <game>/schematics/uploaded;   file = <owner>/<name>.nbt   // 服务端打印用这个
} else {
    dir  = <game>/schematics;            file = <name>.nbt           // 客户端画虚影用这个
}
```

所以 mod 想发一张蓝图，**唯一要做的就是把文件放到 Create 会去找的地方** ✓
大炮、蓝图桌、旋转镜像、虚影预览**全部照常工作** ✓ 一行 Create 的代码都不用碰 ✓

### ⚠️ 藏在子目录里 —— 这样蓝图桌看不到它

```java
Files.list(CreatePaths.SCHEMATICS_DIR)          // ← 不递归
    .filter(f -> !Files.isDirectory(f) && f.getFileName().toString().endsWith(".nbt"))
```

**蓝图桌只扫根目录那一层** ✓ 所以：

| | 路径 |
|---|---|
| 客户端 | `<game>/schematics/`**`enigma/`**`first_generation.nbt` |
| 服务端 | `<game>/schematics/uploaded/create_enigma/`**`enigma/`**`first_generation.nbt` |
| 物品组件 | `create:schematic_file` = `"enigma/first_generation.nbt"`<br>`create:schematic_owner` = `"create_enigma"` |

**这不是钻空子**：Create 自己的路径解析**明确允许子目录** ✓ 只挡 `..` 穿越 ✓

> **两个位置都要写** ✓ ——服务端负责打印、客户端负责画虚影，只写一个会出现
> "能放下去但看不见虚影"或者反过来 ✓

### 名称可以改，贴图**改不了**

实机反馈："它现在下面直接写着 `enigma/first_generation.nbt`，说实话看起来不太聪明" ✓

**能做和不能做，取决于两处 Create 的代码：**

**① 名称 —— 能改** ✓

用 `minecraft:item_name` 组件 ✓ ——**不是** `custom_name` ✓：

```java
stack.set(DataComponents.ITEM_NAME, Component.translatable("item.create_enigma.enigma_blueprint"));
```

`custom_name` 会**渲染成斜体** ✗（那是铁砧改名的语义 ✓）
`item_name` 替换名字**且不斜体** ✓ ——而且它是**组件**，所以**物品展示框、聊天栏、大炮界面里都是这个名字** ✓
不是只在 tooltip 里好看 ✓ ——而且是 `translatable` ✓ 跟随语言 ✓

**② tooltip 里的文件路径 —— 能删** ✓

`ItemTooltipEvent` ✓ 里把那行删掉 ✓ 换成我们自己的话 ✓

**顺序是确认过的** ✓ ——`ItemStack.getTooltipLines` 里：

```java
list.add(名字);                            // ← item_name
this.getItem().appendHoverText(...);       // ← Create 在这里加那行金色文件名
...
EventHooks.onItemTooltip(...);             // ← 我们的钩子在这里，之后
```

**是按内容删的，不是按位置** ✓ ——Create 哪天在上面再加一行也不会崩 ✓

**③ 贴图 —— 做不到** ✗

**两个各自都足够的理由：**

1. **1.21.1 没有 `item_model` 组件** ✗（那是 1.21.4 的东西 ✓）
   → 物品的模型**只由注册名决定** ✓ 换不了 ✓
2. **而且我们必须继续用 `create:schematic`** ✗ ——大炮和手持逻辑用的是**精确物品判定**：

```java
// SchematicHandler.findBlueprintInHand
if (!AllItems.SCHEMATIC.isIn(stack))     // ← 不是 instanceof
    return null;
```

→ **注册我们自己的蓝图物品，大炮根本认不出来** ✗

**唯一的绕法是替换 Create 的 `schematic` 模型**、在里面加一条按 `custom_model_data` 的 override ✓
——但那会**影响所有蓝图** ✗ 而且 Create 更新模型就得跟着改 ✗ **不值得** ✓

**所以现在的效果**：名字是「**厄尼格默的图纸**」✓ tooltip 是「它还记得自己该长什么样。」✓
按住 Shift 是完整说明 ✓ ——**贴图仍然是 Create 的蓝图** ✓

### ⚠️⚠️ "传送带和传动杆" —— 我错了**三次**，最后是**你给了正确答案**

**第一次**："你删传送带的时候把传送带上的传动杆也跟着删了" ✓
我的判断：**"滚轮是 `create:belt[part=pulley]`，所以无法避免"** ✗ ——**错了** ✗

**第二次**：你说"我保存了一个 test.nbt，你对比一下，这个好像才是只去掉传送带的效果" ✓
对比结果**一目了然**：

| | 你的 `test.nbt` | 我们（错） | 完整机器 |
|---|---|---|---|
| `create:shaft` | **11** | **3** | 3 |
| `create:belt` | 0 | 0 | 19 |
| 总数 | **65** | 57 | 83 |

**差值正好 8 根** ✓ ——而那 8 个位置在机器里是**皮带的两端**：

| 位置 | 机器里 | 你的文件里 |
|---|---|---|
| `(7,0,2)` | `belt[part=start, facing=north, slope=upward]` | **`shaft[axis=x]`** |
| `(7,0,8)` | `belt[part=end, facing=south]` | **`shaft[axis=x]`** |
| `(11,1,3)` | `belt[part=start, slope=sideways]` | **`shaft[axis=y]`** |
| `(12,0,7)` | `belt[part=end, facing=south]` | **`shaft[axis=x]`** |

**真相**：`19 = 8 个端点 + 11 个中间段` ✓

> **传送带的两个端块，本身就是"绕过传动杆的那一圈皮带"** ✓
> **拆掉皮带，那里该留下一根传动杆** ✓ ——**只有中间段才什么都不留** ✓
>
> 我上一版说"滚轮是皮带的一部分所以无法避免" ✗ ——
> **无法避免的是"皮带那一圈"消失** ✓ **但下面那根杆是独立的、必须留下** ✓

**杆的轴垂直于皮带走向** ✓（`facing=south` → `axis=x` ✓；`slope=sideways` → `axis=y` ✓）
——这是**从你的文件里读出来的，不是推的** ✓

**为什么这不只是外观问题**：皮带的端块是**绕在杆上的** ✓
把杆一起删掉，**这段皮带就没地方放回去了** ✗ ——玩家会卡在那里 ✓

**而且判定也必须接受"端点位置站着一根杆"** ✓ ——
那正是图纸留下的状态 ✓ 不接受的话，用完图纸的机器**永远没法完成** ✗
所以 `BlockStateMatcher` 加了一条：**皮带的端点位置，放过一根光杆** ✓
（**只对端点** ✓ 中间段站着一根杆是另一种结构，仍然拒绝 ✓）

### ⚠️ 这一轮的验证方式：**逐坐标对比你的文件**

```
你的: 65 块    我们的: 65 块
✓ 逐坐标完全一致
```

**不是我推理"应该对了"，是 65 个方块逐个属性都相同** ✓
——而这是**你搭出来给我看的** ✓ 我只是照着做 ✓

> **教训**：这一轮我推理了三次，错了三次 ✓ 而**你一份存档就解决了** ✓
> 遇到"这个方块到底该是什么"，**找一份实物比想清楚更快** ✓

### ⚠️ 传送带没有"南北之分"——**这是个真的设计缺陷**

实机反馈："提示有一个方块朝向应是南，但现在是北，问题是那个方块是传送带" ✓

**先回答字面问题**：`BeltBlock extends HorizontalKineticBlock` ✓ 所以皮带**确实有** `facing` ✓
属性一共三个：`facing` + `part`（START/MIDDLE/END/PULLEY）+ `slope` ✓

**但那不是玩家选的方向** ✓ ——Create 的皮带是**点一根杆、再点另一根**放出来的 ✓
**先点哪一根，决定了 `facing`，也决定了哪头是 START** ✓

**两个结果都是合法的皮带，铺在同一条线上，只是镜像** ✓ 世界里看不出区别 ✓

**而缺陷在于**：我们**刚把皮带从图纸里去掉了** ✗
→ 玩家**没有任何地方**能看到原设计用的是哪个方向 ✓
→ **要求特定朝向 = 要求猜** ✗ ——而且猜错了**没有任何线索可学** ✓
→ 结果是一台**永远完不成**的机器 ✗✗

**修法**：皮带只比 `slope` ✓，不比 `facing` 和 `part` ✓

```java
"create:belt", Set.of("facing", "part"),
```

**`slope` 保留比对** ✓ ——平地上的皮带和坡道上的皮带是**不同的机器** ✓

**斜的皮带有影响吗？** 没有 ✓ ——斜皮带的方向由**哪一端更高**决定 ✓ 玩家**想错也错不了** ✓
所以忽略 `facing` 不会放过任何真实的错误 ✓

### ⚠️ 一个旧测试挡住了这次修改 —— 而**它错了**

`matcherIgnoresDerivedPropertiesButNotRealChanges` 里有一句：

```java
helper.assertFalse(BlockStateMatcher.matches(belt, cycle(belt, "part")),
        "belt part must NOT be ignored");
```

**这条断言是我早先的假设，不是验证过的事实** ✓ ——"皮带的 part 是真实差异"**听起来很对** ✓
但它错了：**反向铺设时 START 和 END 会互换** ✓ 和 `facing` 一样是玩家控制不了的 ✓

**测试挡住了修改，这次是好事** ✓ ——它逼我把假设变成论证 ✓
改成断言 `part`/`facing` **应当**被忽略、`slope`/`casing` **不应**被忽略 ✓

### ⚠️ 但我**接着又错了一次**：`slope` 里混着两种信息

**第二次反馈**："应该是 DOWNWARD，但实际上是 UPWARD"，**仍然是传送带** ✓
而且"换个传送带连接顺序就行了" ✓

**所以上一节里那句断言是错的** ✗ ——我写了"斜皮带的方向由哪一端更高决定，玩家想错也错不了" ✗
**`UPWARD`/`DOWNWARD` 不是几何形状，是行进方向** ✓ ——Create 自己的代码写着（`BeltBlock:717`）：

```java
// 半圈旋转时，斜皮带的 UPWARD 和 DOWNWARD 互相转换 —— 形状不变
state = state.setValue(SLOPE, slope == UPWARD ? DOWNWARD : slope == DOWNWARD ? UPWARD : slope);
```

**同一段斜坡，先点上面还是先点下面，决定它是 UPWARD 还是 DOWNWARD** ✓ 和 `facing` 一模一样 ✓

**而上一节的修法还留了个更深的漏洞** ✗：我**无条件**忽略了 `facing` ✓ ——
对**平**皮带那是对的 ✓，但对**斜**皮带 `facing` 是**形状的一部分** ✗ 忽略它会放过真正的错误 ✗

**`slope` 同时携带两种信息** ✓ ——所以不能整个忽略 ✗，要**拆开**：

| | |
|---|---|
| **形状** | `HORIZONTAL` / 斜 / `VERTICAL` / `SIDEWAYS` → **必须比对** ✓ |
| **行进方向** | 斜皮带里的 `UPWARD` vs `DOWNWARD` → **归一化掉** ✓ |

现在有一个 `normalise()` ✓ 把斜皮带的两个方向折成同一个 ✓

**于是**：
- 平皮带怎么铺都行 ✓（朝向无关 ✓）
- 斜皮带从上往下点还是从下往上点都行 ✓
- **平地上放一段斜皮带 → 照样拒绝** ✓
- **竖皮带和斜皮带是不同的东西 → 照样拒绝** ✓

> **判据**：如果一条属性**只有靠猜才能弄对**，它就**不属于这台机器** ✓
> 这是两次实机反馈教出来的，不是我想出来的 ✓

### ⚠️ "轮子也跟着删了" —— 是的，而且无法避免

实机反馈："你删传送带的时候把传送带上的传动杆也跟着删了" ✓

**先回答能不能分清**：能 ✓ ——`create:belt` 和 `create:shaft` 是**不同的方块** ✓
过滤器只匹配前者（`AllBlocks.BELT`）✓ 用**同一份模板逐类型数过**：

| 类型 | 图纸 | 机器 |
|---|---|---|
| `create:belt` | **0** | 19 ← 故意去掉 |
| `create_enigma:disguised_motor` | **0** | 6 ← 故意去掉 |
| **`create:shaft`** | **3** | **3** ✓ |
| `create:cogwheel` / `large_cogwheel` / `gantry_shaft` | 2 / 3 / 2 | 2 / 3 / 2 ✓ |
| 其余所有类型 | = | = ✓ |

**传动杆一个没少** ✓ ——而且现在有测试盯着这件事 ✓

**但看到的现象是真的** ✓ ——因为 Create 把皮带的**滚轮**建模成**皮带方块自己的一部分**：

```java
public enum BeltPart { START, MIDDLE, END, PULLEY }
```

`BeltBlockEntity.hasPulley()` ✓ `BeltRenderer` 里 `if (be.hasPulley())` ✓
——**滚轮是一个 `part=pulley` 的 `create:belt` 方块，不是一根独立的杆** ✓

**所以删掉皮带就必然删掉滚轮** ✓ ——想让滚轮留下，就得留下皮带 ✓ ——这是**不可能拆开的** ✓

> **结论：不需要你手工搭一遍存 NBT** ✓
> 过滤器是精确的（有测量 + 测试为证 ✓），消失的是**皮带自带的滚轮** ✓

### ⚠️ 图纸**故意不画全**：传送带和马达留给玩家

蓝图里**没有**这两类方块，原因完全不同：

| 类别 | 为什么去掉 |
|---|---|
| **19 条传送带** | **Create 自己的大炮放不准传送带** ——这是蓝图系统的已知限制，不是这台机器的问题。画进去只会得到一台"到处都是微妙错误传送带"的机器，而手工修那些**比一开始就手工放更糟** |
| **6 台伪装马达** | 画进去等于**一次把整个机关交出去**：它们存在、它们放那儿、它们是替代品。去掉之后，**机器自己有机会说出来**——扳手会报告"只差马达"，并补一句"它现在并不挑食" |

**两类在 `EnigmaStructure.entries()` 里仍然是被要求的** ✓ ——**图纸省略它们，校验器不省** ✓
**这个落差就是玩家的工作** ✓

```
大炮打完          57/82 → 「它正渴求着更多…」
补上 19 条传送带   76/82 → 「它已经饥饿了太久，它现在并不挑食」   ← 铰链在这一步
```

**所以那句提示只会在该出现的时候出现** ✓ ——测试专门断言了这一点：
**图纸单独不能触发"只差马达"** ✓（那 19 条皮带还没放，那句话会是撒谎 ✓）

> 图纸因此从 82 块缩到 **57 块**，文件从 1075 字节缩到 823 字节 ✓

### ⚠️ 图纸是**生成**的，不是随包发的

`EnigmaSchematic.build()` 从 **`EnigmaStructure.entries(Generation.PRIMAL)`** 构建 NBT ✓
——**和校验器读的是同一个列表** ✓

发第二份 `.nbt` 会有漂移风险：**图纸造出来的机器，扳手可能不认** ✗
从同一个来源生成，它们**不可能不一致** ✓

**方块实体数据刻意不带** ✓ ——蓝图的任务是"把对的方块放在对的位置"，
而那正是校验器唯一看的东西 ✓ 马达会以默认转速起来，玩家自己调——和手搭完再做的是同一件事 ✓

### 已核对的

* **12 个测试通过** ✓ 其中一个是蓝图：**把生成的 NBT 读成 `StructureTemplate` 放下去，
  再用扳手那条路读回来** ——不经过我们自己的放置代码 ✓
  这就是"图纸造出来的机器扳手认"的证据，而不是一句声明 ✓
* **蓝图组件的测试以 Create 自己的工厂为基准** ✓ ——`SchematicItem.create()` 产出什么，
  我们就必须有什么 ✓ 用**手写清单**当基准是不够的（第一版就是这么漏掉一个的 ✗）；
  用工厂当基准，将来 Create 加第八个组件时**测试会红**，而不是再崩一次 ✓
* 写出来的文件：**1075 字节、gzip 魔数 `1f 8b`、解压后首字节 `0x0a`** ✓
  ——和 Create 的 `GZIPInputStream` + `NbtIo.read` 完全对得上 ✓
* 文件内容核对：**82 块** ✓ `disguised_motor` ✓ 锚点是 `minecraft:chest` ✓
  **`(6,0,3)` 那台不在** ✓

### ⚠️ 蓝图有**七个**组件，不是两个 —— 这条是崩出来的

第一版只设了**文件名 + 所有者** ✓ 理由是"蓝图就是个文件引用" ✓ ——**听起来很对，但是错的** ✗

实机结果是**拿在手里就崩**：

```
NullPointerException: Cannot invoke "AABB.getCenter()" because
  SchematicHandler.getBounds() is null
    at DeployTool.updateSelection(DeployTool.java:31)
    at SchematicHandler.tick(SchematicHandler.java:118)
```

因为**尺寸不在文件里，在物品上**：

```java
// SchematicHandler:363
Vec3i size = blueprint.get(AllDataComponents.SCHEMATIC_BOUNDS);   // ← 缺了就一直是 null
bounds = new AABB(0, 0, 0, size.getX(), size.getY(), size.getZ());
```

而 `DeployTool` 直接对 `getBounds()` 调 `getCenter()` ✗ ——**Create 自己也没防这个 null** ✓
所以一个不完整的蓝图不是"显示不出来"，是**崩游戏** ✓

**完整的一份**（`SchematicItem.create` 设的全部七个）：

| 组件 | 值 |
|---|---|
| `create:schematic_deployed` | `false` |
| `create:schematic_owner` | `create_enigma` |
| `create:schematic_file` | `enigma/first_generation.nbt` |
| `create:schematic_anchor` | `[0, 0, 0]` |
| `create:schematic_rotation` | `"none"` |
| `create:schematic_mirror` | `"none"` |
| **`create:schematic_bounds`** | **`[15, 4, 15]`** ← 缺的就是它 |

**`EnigmaSchematic.blueprintStack()` 现在设全七个** ✓

> 没有调 Create 的 `SchematicItem.create()`，因为那个方法要一个 `Level` 去读文件算尺寸 ✗
> 而创造标签页构建物品时**没有 level** ✓ 尺寸我们本来就知道（就是模板自己的）✓

### 获取方式是**临时的**

蓝图暂时放在创造模式标签页里 ✓ ——**因为只能靠捡的蓝图是没法测试的** ✓

正式做法是**战利品表** ✓ ——**但必须把七个组件都写全** ✗ 只写两个会做出一个崩游戏的物品：

```json
{
  "type": "minecraft:item",
  "name": "create:schematic",
  "functions": [{ "function": "minecraft:set_components", "components": {
      "create:schematic_deployed": false,
      "create:schematic_owner": "create_enigma",
      "create:schematic_file": "enigma/first_generation.nbt",
      "create:schematic_anchor": [0, 0, 0],
      "create:schematic_rotation": "none",
      "create:schematic_mirror": "none",
      "create:schematic_bounds": [15, 4, 15]
  }}]
}
```

**纯数据，不用写代码** ✓ 放哪个结构还没定 ✓

### 未验证项

* **大炮实机打印没有测过。** 测试走的是"读 NBT → 放下 → 校验"这条路 ✓
  但大炮自己的流程（材料需求、逐块打印、卡住时的行为）没跑过 ✓
* **蓝图桌里确实看不到它** —— 这一条是从 `Files.list` 不递归**推出来的**，
  **没有实机确认** ✓ 你打开蓝图桌看一眼就知道 ✓


