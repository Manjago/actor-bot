package io.github.manjago.engine;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {

    private final AtomicReference<Instant> instant;
    private final ZoneId zone;

    private MutableClock(@NotNull Instant instant, @NotNull ZoneId zone) {
        this.instant = new AtomicReference<>(instant);
        this.zone = zone;
    }

    @Contract("_ -> new")
    public static @NotNull MutableClock of(Instant instant) {
        return new MutableClock(instant, ZoneOffset.UTC);
    }

    public void advance(@NotNull Duration duration) {
        instant.updateAndGet(i -> i.plus(duration));
    }

    public void setTo(@NotNull Instant newInstant) {
        instant.set(newInstant);
    }

    @Override public Instant instant() { return instant.get(); }
    @Override public ZoneId getZone()  { return zone; }

    @Contract("_ -> new")
    @Override
    public @NotNull Clock withZone(ZoneId zone) {
        return new MutableClock(instant.get(), zone);
    }
}