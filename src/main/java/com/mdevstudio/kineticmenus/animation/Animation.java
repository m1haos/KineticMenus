package com.mdevstudio.kineticmenus.animation;

import java.util.List;
import java.util.Map;

/**
 * Frames to draw one per tick and the sounds that go with them.
 */
public record Animation<T>(List<Map<Integer, T>> frames, List<SoundCue> sounds) {

    static <T> Animation<T> still(Map<Integer, T> view) {
        return new Animation<>(List.of(Map.copyOf(view)), List.of());
    }
}
