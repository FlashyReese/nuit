# Square Textured Skybox Layout

`square-textured` uses one texture containing all six cube faces.

The texture is split into a 3 by 2 grid:

| Face id | Face | Grid position |
|---------|------|---------------|
| 0 | bottom | column 0, row 0 |
| 1 | north | column 1, row 1 |
| 2 | south | column 2, row 0 |
| 3 | top | column 1, row 0 |
| 4 | east | column 2, row 1 |
| 5 | west | column 0, row 1 |

Example JSON:

```json
{
  "schemaVersion": 1,
  "type": "square-textured",
  "texture": "example:textures/sky/skybox.png",
  "properties": {
    "blend": {
      "type": "normal"
    }
  }
}
```

For animated or partial-texture layouts, use `multi-textured` with `animatableTextures` instead.
