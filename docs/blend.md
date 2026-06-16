# Blend Modes

Nuit core supports fixed named blend modes.

A blend object only contains `type`:

```json
{
  "type": "normal"
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
| `replace` | Replaces destination color according to source alpha. |
| `disable` | Disables blending for the skybox pipeline. |
| `decorations` | Default blend mode used by sun, moon, and star decorations. |

If `type` is omitted or empty, Nuit uses `normal`.
