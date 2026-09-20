# Nuit Skybox Schema

This document describes Nuit's native resource-pack schema.
The current schema version is `1`.

## File Location

Nuit loads skybox JSON files from client resources under:

```text
assets/nuit/sky/**/*.json
```

Each JSON file defines one skybox layer.

## Minimal Skybox

```json
{
  "schemaVersion": 1,
  "type": "square-textured",
  "texture": "example:textures/sky/skybox.png"
}
```

## Shared Fields

All skybox types share these metadata and optional objects.

| Field | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| `schemaVersion` | integer | yes | none | Current value is `1`. |
| `type` | identifier/string | yes | none | Built-in types may omit the `nuit:` namespace. |
| `properties` | object | no | default properties | Render order, blend, fade, fog, rotation, transitions. |
| `conditions` | object | no | no restrictions | Biome, dimension, skybox, weather, effect, and coordinate checks. |

## Built-In Types

| Type | Purpose |
|------|---------|
| `overworld` | Renders the vanilla overworld sky disc, sunrise/sunset, and dark below-horizon disc. |
| `end` | Renders the vanilla End sky cube. |
| `monocolor` | Renders a solid-color cube. |
| `square-textured` | Renders a six-face skybox from one 3 by 2 texture. |
| `multi-textured` | Renders one or more animated texture regions. |
| `decorations` | Renders custom sun, moon, and/or vanilla stars. |

## Shader Overrides

Nuit skybox shaders are normal client resources. Resource packs can override them by replacing the matching files under `assets/nuit/shaders/core/`.

| Shader | Files | Used by |
|--------|-------|---------|
| `mono_color_skybox` | `mono_color_skybox.vsh`, `mono_color_skybox.fsh` | `monocolor` skyboxes. |
| `textured_skybox` | `textured_skybox.vsh`, `textured_skybox.fsh` | `square-textured`, non-interpolated `multi-textured`, sun, moon, and decoration textures. |
| `multi_textured_skybox` | `multi_textured_skybox.vsh`, `multi_textured_skybox.fsh` | Interpolated `multi-textured` animation frames. |

## `properties`

```json
{
  "layer": 0,
  "blend": "normal",
  "clock": "default",
  "fade": {
    "duration": 24000,
    "keyFrames": {
      "0": 0.0,
      "1000": 1.0,
      "12000": 1.0,
      "13000": 0.0
    }
  },
  "transitionInDuration": 20,
  "transitionOutDuration": 20,
  "sunSkyTint": true,
  "visibleUnderwater": true,
  "fog": {
    "modifyColors": false,
    "red": 0.0,
    "green": 0.0,
    "blue": 0.0,
    "modifyDensity": false,
    "density": 0.0,
    "showInDenseFog": true
  },
  "rotation": {
    "skyboxRotation": true,
    "duration": 24000,
    "speed": 1.0,
    "mapping": {
      "0": [0.0, 0.0, 0.0]
    },
    "axis": {
      "0": [0.0, 1.0, 0.0]
    }
  }
}
```

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `layer` | integer | `0` | Lower layers render first. This replaces old `priority` wording. |
| `blend` | string | `"normal"`; `"decorations"` for decorations | Blend mode for monocolor, textured skyboxes, and sun/moon/star decorations. See [Blend Modes](#blend-modes). |
| `clock` | string or object | `"default"` | Controls the time used by fades and uniform rotation. Most packs can omit it. |
| `fade` | object | empty keyframes, `duration: 24000` | Controls time-of-day alpha. Empty keyframes means always on, subject to conditions. |
| `transitionInDuration` | integer >= 1 | `20` | Condition alpha fade-in duration in ticks. |
| `transitionOutDuration` | integer >= 1 | `20` | Condition alpha fade-out duration in ticks. |
| `fog` | object | no fog modification | Optional fog color/density behavior. |
| `sunSkyTint` | boolean | `true` | If `false`, disables vanilla sunrise/sunset tint contribution for this skybox while rendering. |
| `visibleUnderwater` | boolean | `true` | If `false`, the skybox is hidden underwater. |
| `rotation` | object | no static mapping/axis rotation, `duration: 24000`, `speed: 1.0` | Skybox rotation. |

### `clock`

The `clock` property controls when fades and animated rotation advance. Most packs should leave it out and use the
current dimension's normal clock.

| Value | When to use it |
|-------|----------------|
| Omitted or `"default"` | Recommended. Follow the current dimension's normal clock. |
| `"game_time"` | Advance with gameplay time and ignore changes made with `/time set`. |
| `"minecraft:overworld"` | Follow the Overworld clock, even while the player is in another dimension. |
| `"minecraft:the_end"` | Follow the vanilla End clock. |
| `{ "type": "fixed", "time": 6000 }` | Freeze the fade and rotation at tick `6000`. |
| `{ "type": "clock", "id": "namespace:clock" }` | Follow a custom World Clock supplied by a datapack. |

For example, this keeps a skybox moving with the Overworld clock in the Nether:

```json
{
  "clock": "minecraft:overworld"
}
```

Dimensions without a normal clock, such as the vanilla Nether, keep `"default"` fades and uniform rotation at tick
`0`. Select another clock if they should continue moving there.

Timelines are assigned to dimensions by datapacks, not through Nuit's `clock` setting. Nuit automatically uses the
final sun, moon, star, and weather values produced by Minecraft's active timelines.

#### Advanced clock behavior

Each fade and rotation has its own `duration`. The selected clock supplies the current tick, and Nuit wraps that tick
to the configured duration. World Clocks can be paused or run at a custom rate; Nuit follows their actual movement.
If an explicitly selected clock is missing, Nuit logs a warning and keeps it at tick `0`.

### `fade`

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `duration` | long >= 1 | `24000` | Length of the fade cycle in ticks. |
| `keyFrames` | object of tick string to alpha float | empty | Alpha values are clamped to `0.0` through `1.0`; keyframes must be within `[0, duration)`. |

### `rotation`

Use `skyboxRotation` to choose between an evenly spinning skybox and Minecraft's normal sun movement:

```text
skyboxRotation: true  -> rotate evenly using the selected clock
skyboxRotation: false -> follow Minecraft's sun position
speed: 0              -> disable time-based rotation
```

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `skyboxRotation` | boolean | `true` | `true` spins evenly; `false` follows Minecraft's sun position. |
| `mapping` | object of tick string to `[x, y, z]` degrees | empty | Changes the skybox's base orientation over time. |
| `axis` | object of tick string to `[x, y, z]` degrees | empty | Sets the plane of rotation. Time-based rotation requires at least one axis entry. |
| `duration` | long >= 1 | `24000` | Rotation keyframe cycle length. |
| `speed` | float | `1.0` | With `skyboxRotation: true`, controls rotation speed. `0` disables time-based rotation. |

When `skyboxRotation` is `false`, any nonzero `speed` follows Minecraft's angle; the value does not multiply the
angle. This preserves Nuit's existing resource-pack behavior.

#### Sunrise and fog direction

An active sun decoration with `skyboxRotation` set to `true` can also rotate Minecraft's sunrise colors and fog.

If several such sun decorations are active, they must use the same `clock` and `rotation` settings. Otherwise, Nuit
keeps the dimension's normal sunrise and fog direction and logs a warning.

Moon-only and stars-only decorations do not affect sunrise colors or fog direction.

## `conditions`

```json
{
  "skyboxes": {
    "entries": ["minecraft:overworld"]
  },
  "dimensions": {
    "entries": ["minecraft:overworld"]
  },
  "biomes": {
    "excludes": false,
    "entries": ["minecraft:plains", "nuit:default"]
  },
  "weather": {
    "entries": ["clear"]
  },
  "xRanges": {
    "entries": [{ "min": -100.0, "max": 100.0 }]
  }
}
```

Each condition object has this shape:

```json
{
  "excludes": false,
  "entries": []
}
```

| Field | Entry Type | Notes |
|-------|------------|-------|
| `biomes` | identifiers | Biome identifiers. Supports `nuit:default` fallback behavior. |
| `skyboxes` | identifiers | Vanilla skybox identifiers. Common values: `minecraft:overworld`, `minecraft:end`, `minecraft:none`. |
| `worlds` | identifiers | Legacy compatibility alias. Prefer `skyboxes` for new packs. Legacy values are mapped where possible. |
| `dimensions` | identifiers | Dimension identifiers, e.g. `minecraft:overworld`. |
| `effects` | identifiers | Mob effect identifiers. Empty means default blocked-effect checks apply. |
| `weather` | weather strings | See [Weather Values](#weather-values). |
| `xRanges` | range objects | Player X coordinate ranges. |
| `yRanges` | range objects | Player Y coordinate ranges. |
| `zRanges` | range objects | Player Z coordinate ranges. |

Range objects use inclusive minimum and maximum values:

```json
{ "min": 60.0, "max": 120.0 }
```

## Weather Values

| Value | Meaning |
|-------|---------|
| `clear` | No world rain or thunder. |
| `rain` | World precipitation in biomes without local precipitation. |
| `thunder` | World thunderstorm in biomes without local precipitation. |
| `rain_biome` | Raining in a rain biome. |
| `rain_thunder` | Thunderstorm in a rain biome. |
| `snow` | Snowing in a snow biome. |
| `snow_thunder` | Thunderstorm in a snow biome. |

## Blend Modes

Set the blend mode as a string inside `properties`:

```json
{
  "properties": {
    "blend": "normal"
  }
}
```

Supported modes: `normal`, `alpha`, `add`, `subtract`, `multiply`, `screen`, `burn`, `dodge`, `replace`, `disable`, `decorations`.

When omitted, the default is `decorations` for decoration skyboxes and `normal` for other types. Adding other
properties does not change that default. An empty string explicitly selects `normal`.

See [blend.md](blend.md) for behavior notes.

## Type-Specific Fields

### `monocolor`

```json
{
  "schemaVersion": 1,
  "type": "monocolor",
  "color": {
    "red": 0.1,
    "green": 0.2,
    "blue": 0.4,
    "alpha": 1.0
  },
  "properties": {
    "blend": "normal"
  }
}
```

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `color` | RGBA object | no | `{ red: 0, green: 0, blue: 0, alpha: 0 }` |

RGBA `red`, `green`, `blue`, and optional `alpha` are floats from `0.0` to `1.0`.

### `square-textured`

```json
{
  "schemaVersion": 1,
  "type": "square-textured",
  "texture": "example:textures/sky/skybox.png",
  "properties": {
    "blend": "normal"
  }
}
```

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `texture` | identifier | yes | none |

The texture is interpreted as a 3 by 2 face grid. See [square-textured.md](square-textured.md).

### `multi-textured`

```json
{
  "schemaVersion": 1,
  "type": "multi-textured",
  "properties": {
    "blend": "add"
  },
  "animatableTextures": [
    {
      "texture": "example:textures/sky/cloud_layer.png",
      "uvRange": {
        "minU": 0.0,
        "minV": 0.0,
        "maxU": 1.0,
        "maxV": 1.0
      },
      "gridColumns": 4,
      "gridRows": 4,
      "duration": 50,
      "interpolate": true,
      "frameDuration": {
        "1": 100,
        "2": 50
      }
    }
  ]
}
```

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `animatableTextures` | array of animatable texture objects | no | empty |

#### Animatable Texture

| Field | Type | Default | Notes |
|-------|------|---------|-------|
| `texture` | identifier | required | Sprite sheet texture. |
| `uvRange` | UV range object | full texture | Region of the skybox cube covered by this animation. |
| `gridColumns` | integer >= 1 | `1` | Sprite sheet columns. |
| `gridRows` | integer >= 1 | `1` | Sprite sheet rows. |
| `duration` | long >= 1 | `24000` | Default frame duration in milliseconds. |
| `interpolate` | boolean | `false` | Enables frame blending between current and next frame. |
| `frameDuration` | object of 1-based frame number to milliseconds | empty | Per-frame duration overrides. |

Animation time follows game time plus tick delta. At vanilla 20 TPS, one tick is treated as 50 ms so animations stay tied to world speed.

#### UV Range

```json
{
  "minU": 0.0,
  "minV": 0.0,
  "maxU": 1.0,
  "maxV": 1.0
}
```

UV values are clamped from `0.0` to `1.0`.

### `decorations`

```json
{
  "schemaVersion": 1,
  "type": "decorations",
  "sun": "minecraft:textures/environment/celestial/sun.png",
  "moon": "minecraft:textures/environment/celestial/moon/full_moon.png",
  "showSun": true,
  "showMoon": true,
  "showStars": true,
  "showEndFlash": false,
  "properties": {
    "blend": "decorations"
  }
}
```

| Field | Type | Required | Default |
|-------|------|----------|---------|
| `sun` | identifier | no | vanilla sun texture |
| `moon` | identifier | no | vanilla full moon texture |
| `showSun` | boolean | no | `false` |
| `showMoon` | boolean | no | `false` |
| `showStars` | boolean | no | `false` |
| `showEndFlash` | boolean | no | `false` |

Decoration blending is controlled through `properties.blend` and defaults to `decorations`.
Decoration rotation is controlled through `properties.rotation`. When that object is omitted, decorations use
`skyboxRotation: false`, including when other properties such as `blend` or `layer` are set.

### `overworld` and `end`

These use only shared fields:

```json
{
  "schemaVersion": 1,
  "type": "overworld"
}
```

```json
{
  "schemaVersion": 1,
  "type": "end"
}
```
