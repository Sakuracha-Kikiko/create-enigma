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

## 3. 真正的原因：读错了数据字段（`progress` 而不是 `step`）

> 这一节之前写的是「玩家看不到构件是第几步」。**那是错的**，已推翻：
> Create 本来就会在悬停时显示 `组装进度：n/15`。我上一轮没查就断言它不显示，还写了个
> 重复的 tooltip 类（已删除）。

`SequencedAssembly` 这个组件里有**两个**字段，只有一个在屏幕上：

```java
private int getStep(ItemStack stack) {          // Create 的 tooltip 用的就是它
    if (!stack.has(SEQUENCED_ASSEMBLY)) return 0;
    return stack.get(SEQUENCED_ASSEMBLY).step();  // ← 那个 int
}
```

tooltip 是 `"create.recipe.assembly.progress": "组装进度：%1$s/%2$s"`，
第一个参数 `%1$s` 就是 `step()`，第二个是 `sequence.size() * loops` = **15**。

配方读的却是**另一个字段** `progress()`（一个 float），然后套上我自己编的换算
`round(progress * 13) + 1`。`13` 来自 `getBarWidth()` —— 那是**血条的像素宽度**，
纯粹是画图用的，跟进度刻度毫无关系。

代价：

| 工具提示 | 真实 progress | 旧公式给出 |
|---|---|---|
| `6/15` | 6/15 = 0.4 | `round(5.2)+1 = 6` ✓ |
| `11/15` | 11/15 ≈ 0.733 | `round(9.53)+1 = 11` ✓ |
| **`1/15`** | 1/15 ≈ 0.067 | `round(0.87)+1 = **2**` ✗ |

**`1` 永远配不上。** 而 6/1/11 里就有 1 —— 所以不管摆得多对，永远合不出来。
日志里那个「明明摆的是 1/15，它却说 2」，就是 `round(1/15 × 13) + 1` 的产物。

修法：`content/MechanismStep.java`，直接返回 `assembly.step()` ——
**tooltip 读哪个字段，配方就读哪个字段**，中间不留任何算术，也就没有算错的机会。

## 4. 还没验证的

- 进游戏实际合成一次（服务端已证明能匹配，就差这一步）
- 配方在**客户端**是否也在 recipe manager 里

## 5. 留给以后的

- 排查日志还在 `EnigmaWrenchRecipe` 里（每次 `matches` 打一份网格 + 两种读法）。
  **确认能合出来之后**，删掉 `CALLS`、`refuse`、`logGrid` 和那些 `LOGGER.info`。
- 教训：那个 `+1` 是从血条宽度推出来的，一次都没对着 Create 的源码验过。
  后来三轮改形状、改行列全在修一个不是问题的问题 —— 因为匹配一直是好的。
  凡是要「换算」玩家可见的数字，先把 Create 打印它的那段反汇编出来看。
- 旧证据里有一条值得记住：某次日志显示格子里有 **4 个构件、没有木棍**
  （`2x3 [构件, 构件 | 构件, 齿轮 | 构件, -]`）——那是另一回事，别和进度问题混在一起。
