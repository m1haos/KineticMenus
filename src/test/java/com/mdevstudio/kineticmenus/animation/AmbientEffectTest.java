package com.mdevstudio.kineticmenus.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.animation.ClickEffect.Look;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AmbientEffectTest {

    private static final Grid GRID = new Grid(4);
    private static final Set<Integer> FREE = IntStream.range(0, GRID.size()).boxed()
            .filter(slot -> slot != 3 && slot != 13).collect(Collectors.toSet());

    @Test
    void chaseRunsAlongTheBorderAndSkipsButtons() {
        for (long age = 0; age < 60; age++) {
            Map<Integer, Look> frame = AmbientEffect.CHASE.frame(GRID, FREE, age, 1);
            assertTrue(GRID.border().containsAll(frame.keySet()));
            assertTrue(FREE.containsAll(frame.keySet()));
        }
        assertNotEquals(AmbientEffect.CHASE.frame(GRID, FREE, 0, 1), AmbientEffect.CHASE.frame(GRID, FREE, 1, 1));
    }

    @Test
    void speedHoldsEveryStepForThatManyTicks() {
        assertEquals(AmbientEffect.CHASE.frame(GRID, FREE, 0, 3), AmbientEffect.CHASE.frame(GRID, FREE, 2, 3));
    }

    @Test
    void twinkleLightsEveryFreeSlotSometimesAndNothingElse() {
        Set<Integer> lit = new HashSet<>();
        for (long age = 0; age < 30; age++) {
            Map<Integer, Look> frame = AmbientEffect.TWINKLE.frame(GRID, FREE, age, 1);
            assertTrue(FREE.containsAll(frame.keySet()));
            frame.forEach((slot, look) -> {
                if (look == Look.ACCENT) {
                    lit.add(slot);
                }
            });
        }
        assertEquals(FREE, lit);
    }

    @Test
    void pulseGlowsAndFadesTheWholeBorder() {
        Map<Integer, Look> bright = AmbientEffect.PULSE.frame(GRID, FREE, 2, 1);

        assertTrue(bright.values().stream().allMatch(look -> look == Look.ACCENT));
        assertEquals(GRID.border().stream().filter(FREE::contains).count(), bright.size());
        assertTrue(AmbientEffect.PULSE.frame(GRID, FREE, 0, 1).isEmpty());
    }
}
