package com.mdevstudio.kineticmenus.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.menu.Condition.Operator;
import org.junit.jupiter.api.Test;

class RequirementsTest {

    @Test
    void numbersAreComparedAsNumbers() {
        assertTrue(Requirements.compare("10", Operator.AT_LEAST, "9.5"));
        assertTrue(Requirements.compare("10.0", Operator.EQUAL, "10"));
        assertFalse(Requirements.compare("9", Operator.GREATER, "10"));
        assertTrue(Requirements.compare(" 3 ", Operator.LESS, "4"));
    }

    @Test
    void textOnlySupportsEqualityAndNeverPassesOrderChecks() {
        assertTrue(Requirements.compare("world_nether", Operator.EQUAL, "world_nether"));
        assertTrue(Requirements.compare("world", Operator.NOT_EQUAL, "world_nether"));
        assertFalse(Requirements.compare("%player_level%", Operator.AT_LEAST, "10"));
    }
}
