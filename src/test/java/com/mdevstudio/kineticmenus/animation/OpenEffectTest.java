package com.mdevstudio.kineticmenus.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

class OpenEffectTest {

    private static final Grid GRID = new Grid(5);

    private static Map<Integer, String> fullMenu() {
        Map<Integer, String> layout = new HashMap<>();
        for (int slot = 0; slot < GRID.size(); slot++) {
            layout.put(slot, "item" + slot);
        }
        return layout;
    }

    @ParameterizedTest
    @EnumSource(OpenEffect.class)
    void everyEffectEndsOnTheExactLayout(OpenEffect effect) {
        Map<Integer, String> layout = fullMenu();

        List<Map<Integer, String>> frames = effect.frames(layout, GRID, 12, new Random(1));

        assertEquals(layout, frames.getLast());
    }

    @ParameterizedTest
    @EnumSource(value = OpenEffect.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
    void effectsLastTheRequestedNumberOfTicks(OpenEffect effect) {
        assertEquals(12, effect.frames(fullMenu(), GRID, 12, new Random(1)).size());
    }

    @Test
    void cascadeShowsTheTopRowFirstAndTheBottomRowLast() {
        List<Map<Integer, String>> frames = OpenEffect.CASCADE.frames(fullMenu(), GRID, 5, new Random(1));

        assertEquals(Grid.COLUMNS, frames.getFirst().size());
        assertTrue(frames.getFirst().keySet().stream().allMatch(slot -> GRID.row(slot) == 0));
        assertTrue(frames.get(3).keySet().stream().noneMatch(slot -> GRID.row(slot) == 4));
    }

    @Test
    void waveStartsInTheMiddle() {
        Map<Integer, String> first = OpenEffect.WAVE.frames(fullMenu(), GRID, 10, new Random(1)).getFirst();

        assertTrue(first.containsKey(GRID.slot(2, 4)));
        assertTrue(!first.containsKey(0));
    }

    @Test
    void revealedSlotsNeverDisappear() {
        List<Map<Integer, String>> frames = OpenEffect.SPIRAL.frames(fullMenu(), GRID, 15, new Random(1));

        for (int tick = 1; tick < frames.size(); tick++) {
            assertTrue(frames.get(tick).keySet().containsAll(frames.get(tick - 1).keySet()));
        }
    }

    @Test
    void spiralVisitsEverySlotOnceStartingAtTheCorner() {
        Map<Integer, Integer> order = OpenEffect.spiralOrder(GRID);

        assertEquals(GRID.size(), order.size());
        assertEquals(GRID.size(), new HashSet<>(order.values()).size());
        assertEquals(0, order.get(0));
        assertEquals(Grid.COLUMNS, order.get(GRID.slot(1, 8)));
    }

    @Test
    void scatterMovesItemsWithoutInventingNewOnes() {
        Map<Integer, String> layout = Map.of(10, "a", 13, "b", 16, "c");

        for (Map<Integer, String> frame : OpenEffect.SCATTER.frames(layout, GRID, 10, new Random(7))) {
            assertTrue(layout.values().containsAll(frame.values()));
            assertTrue(frame.keySet().stream().allMatch(slot -> slot >= 0 && slot < GRID.size()));
        }
    }

    @Test
    void shuffleOnlyUsesSlotsOfTheMenu() {
        Map<Integer, String> layout = Map.of(10, "a", 13, "b", 16, "c");

        for (Map<Integer, String> frame : OpenEffect.SHUFFLE.frames(layout, GRID, 8, new Random(3))) {
            assertEquals(layout.keySet(), frame.keySet());
        }
    }

    @Test
    void emptyMenuOrSingleTickIsShownAtOnce() {
        assertEquals(List.of(Map.of()), OpenEffect.SCATTER.frames(Map.of(), GRID, 10, new Random(1)));
        assertEquals(1, OpenEffect.WAVE.frames(fullMenu(), GRID, 1, new Random(1)).size());
    }
}
