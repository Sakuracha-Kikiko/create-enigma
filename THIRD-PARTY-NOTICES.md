# Third-party notices

This mod is MIT licensed (see `LICENSE`). One file in it is a derivative work of another project's
asset, and this file carries the notice that the MIT license requires.

---

## Create — MIT

- **Project:** Create (https://github.com/Creators-of-Create/Create)
- **License:** MIT — Copyright (c) the Creators of Create
- **Derived file:** `src/main/resources/assets/create_enigma/textures/item/enigma_wrench.png`
- **Source file:** `assets/create/textures/item/wrench.png`, from the Create build this mod depends on
- **What was changed:** colour only. The original's warm span (gold into brown) is remapped onto
  violet and magenta-violet; hue variation, brightness and alpha are preserved, and near-grey metal
  pixels are untouched. The image is still 16×16 and every UV island still sits exactly where
  Create's wrench model expects it.
- **Why a copy rather than a reference:** the wrench's geometry is referenced by name — this mod's
  models inherit `create:item/wrench/item` and `create:item/wrench/gear` and copy no geometry. But a
  model cannot recolour itself, so a differently coloured wrench needs a texture file of its own.
- **Reproduce:** `tools/RecolourWrenchTexture.java` regenerates the file from the source texture.

Everything else this mod draws reuses Create's block textures by name at runtime, without
redistributing them; see the "贴图" section of `README.md`.
