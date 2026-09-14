package me.flashyreese.mods.nuit.components.clock.runtime;

import me.flashyreese.mods.nuit.NuitClient;
import me.flashyreese.mods.nuit.components.clock.ClockSource;
import me.flashyreese.mods.nuit.components.clock.runtime.source.MissingTimeSource;
import me.flashyreese.mods.nuit.components.clock.runtime.source.ResolvedTimeSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.timeline.Timeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LevelClockState {
    protected final Map<ClockSource, TickSample> samples = new HashMap<>();
    protected final Set<MissingTimeSource> warnedMissingTimeSources = new HashSet<>();

    public ResolvedTimeSource resolvedClock(ClientLevel level, Holder<WorldClock> clock) {
        return new ResolvedTimeSource(true, level.clockManager().getInstance(clock).totalTicks(), clock);
    }

    public void warnMissing(ClientLevel level, Identifier id) {
        MissingTimeSource missingTimeSource = new MissingTimeSource(id);
        if (this.warnedMissingTimeSources.add(missingTimeSource)) {
            ResourceKey<Timeline> timelineKey = ResourceKey.create(Registries.TIMELINE, id);
            if (level.registryAccess().get(timelineKey).isPresent()) {
                NuitClient.getLogger().warn("Skybox time source {} is a timeline, not a World Clock. Assign timelines to dimensions through a datapack instead of Nuit's clock property.", id);
            } else {
                NuitClient.getLogger().warn("Skybox time source references missing world clock {}. It will remain at tick 0 until the entry exists.", id);
            }
        }
    }

    public TickSample getTickSample(ClockSource clockSource) {
        return this.samples.computeIfAbsent(clockSource, ignored -> new TickSample());
    }

    public Set<MissingTimeSource> warnedMissingSources() {
        return this.warnedMissingTimeSources;
    }
}
