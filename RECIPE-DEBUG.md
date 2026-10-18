# 神秘扳手配方 — 排查结论

> 这个文件曾经很长，记录了一次跨越很多轮的排查。结论已经确定，所以只剩结论。
> 之前那版中文被 PowerShell 读写往返弄成了乱码，已重写。

---

## 1. 配方本身没问题（有证据）

测试 `wrenchRecipeMatchesTheGridTheSceneDescribes` 构造了**和你摆的一模一样的格子**
（三个构件在左上/上中/左中，齿轮在正中，木棍在下中），进度用的就是这个世界真正的三个数，
然后走 `RecipeManager.getRecipeFor(RecipeType.CRAFTING, ...)` —— 也就是工作台问的同一个问题。

结果：**匹配上了**，偏移 0,0。日志原文：

```
Enigma recipe #1:     mechanism 13/15   mechanism 3/15
Enigma recipe #1:     mechanism 12/15   create:cogwheel
Enigma recipe #1:     .                 minecraft:stick
Enigma recipe #1: MATCHED at offset 0,0
```

所以形状、偏移扫描、材料、进度比较，全对。

## 2. `CraftingInput.getItem` 的参数名是错的

存储是行优先（`列 + 行 * 宽`），访问器却读 `行 + 列 * 宽`。**第一个参数实际是列。**
现在全部读取走 `EnigmaWrenchRecipe.at(input, x, y)`，日志里同时打印两种读法：

- `getItem(column, row)` → 正确的表
- `getItem(row, column)` → 它的镜像（明显是乱的）

这是**量出来的**，不是推出来的。

## 3. 真正的原因：玩家看不到构件是第几步

整个 mod（以及 Create）**从来没有显示过一个未完成精密构件的步骤数**。
Create 画的是 13 像素宽的条，没有数字，凭肉眼分不出第 6 步和第 7 步。

于是「拿三个特定步骤的构件」不是一个谜题，是一次**猜**——
而差一步和差十二步，表现出来完全一样（合不出来，没有任何提示）。

**这就是为什么前三轮改形状、改行列都没有用：匹配一直是好的，是玩家拿不到对的东西。**

修法：`client/IntermediateTooltip.java`，悬停构件显示「第 n / 15 步」，
公式和配方里 `progressAt` **逐字符相同**（`round(progress * 13) + 1`）。
两边一旦不一致，所有正确尝试都会失败且游戏不会说任何原因——这个类就是为了终结那种失败。

## 4. 还没验证的

- 客户端上这行 tooltip 实际渲染出来是什么样（只能在游戏里看）
- 配方在**客户端**是否也在 recipe manager 里（服务端已证实在）

## 5. 留给以后的

- 排查日志还在 `EnigmaWrenchRecipe` 里（每次 `matches` 打一份网格）。修好之后删掉 `CALLS` 和那些 `LOGGER.info`。
- 旧证据里有一条值得记住：某次日志显示格子里有 **4 个构件、没有木棍**
  （`2x3 [构件, 构件 | 构件, 齿轮 | 构件, -]`）——那是另一回事，别把它和进度问题混在一起。
- 详情见 git 提交 `Log every recipe test, prove the match in a gametest, and show the mechanism's step`。
