package com.mdevstudio.kineticmenus.animation;

/**
 * Slot arithmetic for a chest menu: rows of nine slots, numbered left to right, top to bottom.
 */
public record Grid(int rows) {

    public static final int COLUMNS = 9;

    public Grid {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("A chest menu has 1 to 6 rows, got " + rows);
        }
    }

    public int size() {
        return rows * COLUMNS;
    }

    public int row(int slot) {
        return slot / COLUMNS;
    }

    public int column(int slot) {
        return slot % COLUMNS;
    }

    public int slot(int row, int column) {
        return row * COLUMNS + column;
    }

    public boolean contains(int row, int column) {
        return row >= 0 && row < rows && column >= 0 && column < COLUMNS;
    }

    public double centerRow() {
        return (rows - 1) / 2.0;
    }

    public double centerColumn() {
        return (COLUMNS - 1) / 2.0;
    }
}
