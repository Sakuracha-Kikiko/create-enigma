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

### Also derived: the creative hand crank's three textures

- **Derived files**, all in `src/main/resources/assets/create_enigma/textures/block/`:
  `creative_hand_crank_axis.png`, `creative_hand_crank_casing.png`,
  `creative_hand_crank_log_top.png`
- **Source files**, from the same Create build: `assets/create/textures/block/axis.png`,
  `andesite_casing_short.png`, `smooth_dark_log_top.png`
- **What was changed:** colour only, and by a different method than the wrench. Hue rotation does
  nothing to andesite casing (it is grey, with no saturation to rotate), and multiplying by a violet
  leaves the dark log maroon (it is red-dominant, so scaling green and blue down keeps red on top).
  So the colour is discarded and rebuilt from brightness: each pixel's luminance is mapped onto a
  dark-violet to light-violet ramp. Shading, edges and highlights survive because luminance is
  untouched; the material distinction between bare metal and wood does not, which is intended - the
  point is that the whole thing reads as a creative item.
- **Reproduce:** `tools/Tint.java`.

### Geometry referenced, never copied

The creative hand crank's arm and base, and the Enigma Wrench's handle and gear, are model files in
this mod that contain **no geometry**: each is a `parent` plus a texture rebinding, so the shapes are
inherited from Create's own model files at runtime.

Everything else this mod draws reuses Create's block textures by name at runtime, without
redistributing them; see the "贴图" section of `README.md`.
