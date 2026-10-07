package com.mdevstudio.kineticmenus.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.animation.ClickEffect.Look;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ClickEffectTest {

    private static final Grid GRID = new Grid(3);
    private static final int MIDDLE = GRID.slot(1, 4);
    private static final Set<Integer> ALL_FREE = IntStream.range(0, GRID.size()).boxed()
            .filter(slot -> slot != MIDDLE).collect(Collectors.toSet());

    @Test
    void rippleNeverCoversOtherButtons() {
        Set<Integer> free = Set.of(GRID.slot(1, 3), GRID.slot(0, 0));

        for (Map<Integer, Look> frame : ClickEffect.RIPPLE.frames(GRID, MIDDLE, free)) {
            assertTrue(free.containsAll(frame.keySet()));
        }
    }

    @Test
    void rippleGrowsRingByRingAndClearsAfterwards() {
        List<Map<Integer, Look>> frames = ClickEffect.RIPPLE.frames(GRID, MIDDLE, ALL_FREE);

        assertEquals(8, frames.getFirst().size());
        assertTrue(frames.getLast().isEmpty());
    }

    @Test
    void shakeMovesTheButtonSidewaysAndHidesItsSlot() {
        Map<Integer, Look> first = ClickEffect.SHAKE.frames(GRID, MIDDLE, ALL_FREE).getFirst();

        assertEquals(Look.HIDDEN, first.get(MIDDLE));
        assertEquals(Look.BUTTON, first.get(MIDDLE - 1));
    }

    @Test
    void shakeAtTheEdgeKeepsItsRhythmWithoutLeavingTheMenu() {
        List<Map<Integer, Look>> frames = ClickEffect.SHAKE.frames(GRID, 0, Set.of(1));

        assertEquals(4, frames.size());
        assertEquals(Map.of(0, Look.HIDDEN), frames.getFirst());
    }

    @Test
    void ringsAreSquaresAroundTheCenter() {
        assertEquals(8, ClickEffect.ring(GRID, MIDDLE, 1).size());
        assertEquals(3, ClickEffect.ring(GRID, 0, 1).size());
    }
}
