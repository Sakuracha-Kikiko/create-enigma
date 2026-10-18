# 神秘扳手配方 —— 排查状态（临时文件，问题解决后删除）

> 这个文件的存在理由：排查过程跨越了很多轮，而模型的上下文会压缩。
> 把**已确认的事实**和**未解的矛盾**写在这里，就不依赖记忆了。

---

## 已确认（都有证据）

### 1. 配方本身注册正确

测试 `wrenchRecipeLoadsAndProducesTheWrench` 断言并通过：

- `RecipeManager.byKey(create_enigma:enigma_wrench)` 能拿到 ✓
- `getType() == RecipeType.CRAFTING` ✓
- 产物是 `create_enigma:enigma_wrench` ✓
- `isSpecial() == false` ✓（JEI 会跳过 isSpecial 为 true 的配方）
- `getIngredients()` 五个材料各自接受什么 ✓

### 2. `CraftingInput` 的语义（读过源码 `CraftingInput.java`）

```java
// 行在前，列在后；步长是 width
public ItemStack getItem(int row, int column) {
    return this.items.get(row + column * this.width);
}
```

**`getItem(y, x)` 的写法是对的** ✓

**关键**：`CraftingInput.of(width, height, items)` **不是原样返回** ✗
它会调用 `ofPositioned(...)`，把网格**裁剪到"有物品的最小矩形"**。
所以 3×3 的合成台，如果物品只占了 2 列 × 3 行，传给配方的是 **2×3**。
——日志里的 `2x3` 就是这么来的，**不是 JEI 的假网格**。

### 3. Create 的半成品进度

```java
// SequencedAssemblyItem
public float getProgress(ItemStack stack) { ... }
public int getBarWidth() { return Math.round(getProgress(stack) * 13); }   // 0..13
```

**源码里没有任何 "x/15" 的文字** —— 那个读法来自看着进度条长度。
内部只有 **0..13 共 14 档**，与"1/15 到 14/15"一一对应，差 1。
`EnigmaCode` 对外用 1..14，配方比对时 `round(p*13) + 1`。

### 4. 数字同步链路正常

日志里出现过：
```
EnigmaCodeSyncClient: this world's numbers are 6, 1, 11.
```
服务端按种子算出 ✓ 发到客户端 ✓ 思索场景也用同一份 ✓

---

## 未解的矛盾 ✗

用户说：摆的是 **3 个半成品（6/1/11）+ 齿轮 + 木棍**，按配方形状摆。

日志说（最后几条，`Enigma recipe: refused`）：
```
2x3 [半成品, 半成品 | 半成品, 齿轮 | 半成品, -]
wanted=[6, 1, 11]  steps=[6 11 2 11]
```

→ **4 个半成品、没有木棍、进度是 6/11/2/11**

**两个解释**：

- **A**（更可能）：那行日志来自用户**中途某次尝试**，不是最后那次。用户试过多次。
- **B**：最后一次没进日志（`slotsChanged` 没触发）。

---

## 下一步（只做这一件）

**让日志只记录一次干净的尝试**，然后照着那一条判断：

1. 让 `matches` 的日志带上**每次调用递增的序号**，并打印**完整网格**（含空格）
2. 请用户：进存档 → 摆一次 → 立刻退出 → 不要做别的
3. 日志里应该只有**一条**记录，那就是答案

### 判断表

| 日志里看到 | 结论 |
|---|---|
| `grid` 里是 3 个半成品 + 齿轮 + 木棍，`steps` 对得上 `wanted` | 那是**空位判定或偏移扫描**的问题 |
| `steps` 对不上 `wanted` | **进度换算**错了（检查 `+1` 方向） |
| `grid` 里明显缺东西 | 不是代码问题，是物品没摆进去 |
| 一条都没有 | `slotsChanged` 没走到 → 要换插桩位置 |

---

## 方法上的教训（写给下一轮的我）

- **先读源码，再改代码**。这一轮改了三轮形状，全都在一个自己没读过的 API 上。
- **一次只改一处**，并且**先有能复现的证据**。
- 用户说的现象**一直是对的**；错的是我对观测的解释。
