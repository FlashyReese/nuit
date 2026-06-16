# Nuit API

Nuit exposes a small client-side API for mods that want to register skybox schemas or manage skyboxes at runtime.
The public API lives under `me.flashyreese.mods.nuit.api` and `me.flashyreese.mods.nuit.api.skyboxes`.

The API is beta for the 1.0.0 line. Prefer the documented entry points below and avoid depending on internal render classes.

## Dependency

Use the loader artifact for the platform you are building against.

```gradle
repositories {
    maven {
        url = "https://maven.flashyreese.me/snapshots"
    }
}

dependencies {
    modImplementation "me.flashyreese.mods:nuit-fabric:${nuit_version}"
    // or: modImplementation "me.flashyreese.mods:nuit-neoforge:${nuit_version}"
}
```

## Main Entry Point

```java
NuitApi api = NuitApi.getInstance();
```

`NuitApi.getApiVersion()` currently returns `1`.

## Registering A Skybox Type

The canonical registration path is `NuitApi.registerSkyboxType(...)`.
```java
public final class ExampleClient {
    public static final SkyboxType<ExampleSkybox> EXAMPLE_SKYBOX = NuitApi.registerSkyboxType(
            Identifier.fromNamespaceAndPath("example", "example_skybox"),
            1,
            ExampleSkybox.CODEC
    );
}
```

Skybox type identifiers are normal Minecraft identifiers. If the namespace is omitted in resource JSON, Nuit resolves the type under the `nuit` namespace.

## Runtime Skyboxes

```java
NuitApi api = NuitApi.getInstance();
Identifier id = Identifier.fromNamespaceAndPath("example", "runtime_skybox");

api.addSkybox(id, skybox);              // cleared on resource reload
api.addPermanentSkybox(id, skybox);     // survives resource reload
api.removeSkybox(id);
api.removePermanentSkybox(id);
api.getSkybox(id);
api.getSkyboxes();
api.getActiveSkyboxes();
api.getCurrentSkybox();                 // Optional<Skybox>
```

Use `parseSkybox(id, jsonObject)` when you want Nuit to decode JSON without immediately adding the result.

## Skybox Interfaces

### `Skybox`

Base lifecycle contract:

```java
public interface Skybox {
    default int getLayer() { return 0; }
    void tick(ClientLevel level);
    boolean isActive();
}
```

Lower layers render first.

### `RenderableSkybox`

Implement this when the skybox draws during Nuit's sky render pass:

```java
public interface RenderableSkybox extends Skybox {
    void render(SkyboxRenderContext context);
}
```

### `NuitSkybox`

Use this for Nuit-style skyboxes with alpha, properties, and conditions:

```java
public interface NuitSkybox extends RenderableSkybox {
    float getAlpha();
    void updateAlpha(ClientLevel level);
    Properties getProperties();
    Conditions getConditions();
}
```

`AbstractSkybox` already implements the standard Nuit alpha and condition behavior.

### `SkyboxTextureProvider`

Implement this when Nuit should preload and release textures used by your skybox:

```java
public interface SkyboxTextureProvider {
    Collection<Identifier> getTexturesToRegister();
}
```

## Render Context

`SkyboxRenderContext` is the public render boundary. It exposes the frame state and stable vanilla helpers Nuit supports:

```java
context.matrixStack();
context.tickDelta();
context.camera();
context.applyFog();
context.renderSkyDisc(color);
context.renderDarkDisc();
context.renderStars(brightness, poseStack);
context.renderEndFlash(intensity, xAngle, yAngle);
context.endSkyTexture();
```

Do not depend on Nuit's internal `NuitRenderBackend` or `NuitRenderPipelines` as public API unless you are working inside Nuit itself. Those classes may change with Minecraft renderer changes.

## Minimal Renderable Example

```java
public final class ExampleSkybox implements RenderableSkybox {
    public static final Codec<ExampleSkybox> CODEC = Codec.unit(ExampleSkybox::new);

    @Override
    public void tick(ClientLevel level) {
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public int getLayer() {
        return 100;
    }

    @Override
    public void render(SkyboxRenderContext context) {
        context.applyFog();
        // Draw sky geometry here, or call supported context helpers.
    }
}
```

## JSON For A Registered Type

```json
{
  "schemaVersion": 1,
  "type": "example:example_skybox"
}
```
