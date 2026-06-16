# Square Textured Skybox Layout

`square-textured` uses one texture containing all six cube faces.

The texture is split into a 3 by 2 grid:

| Face id | Grid position |
|---------|---------------|
| 0 | column 0, row 0 |
| 1 | column 1, row 0 |
| 2 | column 2, row 0 |
| 3 | column 0, row 1 |
| 4 | column 1, row 1 |
| 5 | column 2, row 1 |

Example JSON:

```json
{
  "schemaVersion": 1,
  "type": "square-textured",
  "texture": "example:textures/sky/skybox.png",
  "blend": {
    "type": "normal"
  }
}
```

For animated or partial-texture layouts, use `multi-textured` with `animatableTextures` instead.
