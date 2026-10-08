package com.mdevstudio.kineticmenus.menu;

/**
 * Something about the player that is checked before a menu opens, a button shows or a click goes through.
 */
public sealed interface Condition {

    record Permission(String node) implements Condition {
    }

    /**
     * Needs Vault and an economy plugin; without them the condition is never met.
     */
    record Money(double amount) implements Condition {
    }

    /**
     * Both sides may hold placeholders. Numbers are compared as numbers, anything else as text, where only
     * {@code ==} and {@code !=} make sense.
     */
    record Compare(String left, Operator operator, String right) implements Condition {
    }

    record Not(Condition condition) implements Condition {
    }

    enum Operator {
        EQUAL("=="),
        NOT_EQUAL("!="),
        AT_LEAST(">="),
        AT_MOST("<="),
        GREATER(">"),
        LESS("<");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }
    }
}
