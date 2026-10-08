package com.mdevstudio.kineticmenus.animation;

/**
 * Speed curve of a movement over its duration.
 */
public enum Easing {

    LINEAR,
    /** Starts slowly and speeds up, like something falling. */
    EASE_IN,
    /** Starts fast and settles gently. */
    EASE_OUT,
    /** Gentle at both ends. */
    EASE_IN_OUT;

    public double apply(double progress) {
        double x = Math.clamp(progress, 0, 1);
        return switch (this) {
            case LINEAR -> x;
            case EASE_IN -> x * x * x;
            case EASE_OUT -> 1 - Math.pow(1 - x, 3);
            case EASE_IN_OUT -> x < 0.5 ? 4 * x * x * x : 1 - Math.pow(2 - 2 * x, 3) / 2;
        };
    }
}
