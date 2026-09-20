# Blend Modes

Nuit core supports fixed named blend modes.

Set the mode as a string in `properties.blend`:

```json
{
  "properties": {
    "blend": "normal"
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
| `replace` | Replaces destination color according to source alpha. |
| `disable` | Disables blending for the skybox pipeline. |
| `decorations` | Default blend mode used by sun, moon, and star decorations. |

Omitting `blend` uses `decorations` for decoration skyboxes and `normal` for other types. An empty string selects
`normal` explicitly. This setting controls monocolor, textured skyboxes, and sun/moon/star decorations.
