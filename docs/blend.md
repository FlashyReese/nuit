# Blend Modes

Nuit core supports fixed named blend modes.

A blend object is configured as `properties.blend`:

```json
{
  "properties": {
    "blend": {
      "type": "normal"
    }
  }
}
```

## Supported Types

| Type | Description |
|------|-------------|
| `normal` | Standard alpha blending. Alias: `alpha`. |
| `add` | Additive blending. Useful for glows, stars, and light overlays. |
| `subtract` | Subtractive-style fixed-function blend. |
| `multiply` | Multiplies against the destination color. |
| `screen` | Screen-like fixed-function blend. |
| `burn` | Burn-like fixed-function blend. |
| `dodge` | Dodge-like fixed-function blend. |
| `replace` | Uses the legacy `ZERO, ONE` fixed-function pair, which preserves the destination color. |
| `disable` | Disables blending for the skybox pipeline. |
| `decorations` | Default blend mode used by sun, moon, and star decorations. |
| `custom` | Uses the 1.21.1-only `blender` object described below. |

If `type` is omitted or empty, Nuit uses `normal`.

## Custom Blender (Minecraft 1.21.1)

Minecraft 1.21.1 still exposes the OpenGL blend state that newer render pipelines replace. This branch therefore
retains the legacy `custom` type:

```json
{
  "type": "custom",
  "blender": {
    "separateFunction": false,
    "sourceFactor": 770,
    "destinationFactor": 771,
    "equation": 32774,
    "sourceFactorAlpha": 0,
    "destinationFactorAlpha": 0,
    "redAlphaEnabled": false,
    "greenAlphaEnabled": false,
    "blueAlphaEnabled": false,
    "alphaEnabled": true
  }
}
```

`sourceFactor` and `destinationFactor` are passed to `glBlendFunc`. When `separateFunction` is `true`,
`sourceFactorAlpha` and `destinationFactorAlpha` are also passed to `glBlendFuncSeparate`. `equation` is passed to
`glBlendEquation`.

The four `*AlphaEnabled` fields select whether the corresponding shader-color component receives the skybox alpha
(`true`) or `1.0` (`false`). Invalid factors or equations fall back to Minecraft's default blend function.

### Factor Values

| Factor | Value | Source | Destination |
|--------|------:|:------:|:-----------:|
| `CONSTANT_ALPHA` | 32771 | yes | yes |
| `CONSTANT_COLOR` | 32769 | yes | yes |
| `DST_ALPHA` | 772 | yes | yes |
| `DST_COLOR` | 774 | yes | yes |
| `ONE` | 1 | yes | yes |
| `ONE_MINUS_CONSTANT_ALPHA` | 32772 | yes | yes |
| `ONE_MINUS_CONSTANT_COLOR` | 32770 | yes | yes |
| `ONE_MINUS_DST_ALPHA` | 773 | yes | yes |
| `ONE_MINUS_DST_COLOR` | 775 | yes | yes |
| `ONE_MINUS_SRC_ALPHA` | 771 | yes | yes |
| `ONE_MINUS_SRC_COLOR` | 769 | yes | yes |
| `SRC_ALPHA` | 770 | yes | yes |
| `SRC_ALPHA_SATURATE` | 776 | yes | no |
| `SRC_COLOR` | 768 | yes | yes |
| `ZERO` | 0 | yes | yes |

### Equation Values

| Equation | Value |
|----------|------:|
| `ADD` | 32774 |
| `SUBTRACT` | 32778 |
| `REVERSE_SUBTRACT` | 32779 |
| `MIN` | 32775 |
| `MAX` | 32776 |
