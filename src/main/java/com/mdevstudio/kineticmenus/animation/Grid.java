package com.mdevstudio.kineticmenus.animation;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Every slot once, ring by ring from the border inwards, clockwise from the top left corner.
     */
    public List<Integer> rings() {
        List<Integer> order = new ArrayList<>();
        int ring = 0;
        while (addRing(order, ring)) {
            ring++;
        }
        return order;
    }

    /**
     * The outer ring of slots, clockwise from the top left corner.
     */
    public List<Integer> border() {
        List<Integer> order = new ArrayList<>();
        addRing(order, 0);
        return order;
    }

    private boolean addRing(List<Integer> order, int ring) {
        int top = ring;
        int bottom = rows - 1 - ring;
        int left = ring;
        int right = COLUMNS - 1 - ring;
        if (top > bottom || left > right) {
            return false;
        }
        for (int column = left; column <= right; column++) {
            order.add(slot(top, column));
        }
        for (int row = top + 1; row <= bottom; row++) {
            order.add(slot(row, right));
        }
        for (int column = right - 1; column >= left && bottom > top; column--) {
            order.add(slot(bottom, column));
        }
        for (int row = bottom - 1; row > top && right > left; row--) {
            order.add(slot(row, left));
        }
        return true;
    }
}
