package com.mdevstudio.kineticmenus.animation;

import net.kyori.adventure.key.Key;

/**
 * A sound played on a given tick of an animation, counting from its first frame.
 */
public record SoundCue(int tick, Key sound, float volume, float pitch) {

    static SoundCue at(int tick, String sound, double volume, double pitch) {
        return new SoundCue(tick, Key.key(sound), (float) volume, (float) pitch);
    }
}
