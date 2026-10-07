package com.mdevstudio.kineticmenus.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mdevstudio.kineticmenus.animation.ClickEffect;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.menu.Button.ClickKind;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.slf4j.LoggerFactory;

class MenuLoaderTest {

    private final MenuLoader loader = new MenuLoader(LoggerFactory.getLogger(MenuLoaderTest.class));

    @BeforeEach
    void setUp() {
        // Material lookups need a server registry.
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Menu load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return loader.load("test", config, "menus/test.yml");
    }

    @Test
    void readsEffectsSlotsAndActions() throws InvalidConfigurationException {
        Menu menu = load("""
                rows: 2
                open:
                  effect: Spiral
                  ticks: 9
                buttons:
                  row:
                    slots: ["9-11", 13]
                    material: diamond
                    effect: ripple
                    on-click: ["message: hi", "sound: ui.button.click 0.5 2", "open: other scatter"]
                    on-right-click: ["close"]
                """);

        assertEquals(OpenEffect.SPIRAL, menu.openEffect());
        assertEquals(9, menu.openTicks());
        Button button = menu.buttons().getFirst();
        assertEquals(List.of(9, 10, 11, 13), button.slots());
        assertEquals(ClickEffect.RIPPLE, button.effect());
        assertEquals(new Action.Open("other", OpenEffect.SCATTER), button.actionsFor(ClickKind.ANY).get(2));
        assertEquals(List.of(new Action.Close()), button.actionsFor(ClickKind.RIGHT));
        assertEquals(3, button.actionsFor(ClickKind.LEFT).size());
    }

    @Test
    void mistakesAreSkippedInsteadOfBreakingTheMenu() throws InvalidConfigurationException {
        Menu menu = load("""
                rows: 9
                open:
                  effect: explode
                buttons:
                  broken:
                    slots: [5, 99, "x"]
                    material: not_a_block
                    effect: wobble
                    on-click: ["teleport: spawn", "message: ok"]
                  outside:
                    slot: 70
                """);

        assertEquals(3, menu.grid().rows());
        assertEquals(OpenEffect.NONE, menu.openEffect());
        assertEquals(1, menu.buttons().size());
        Button button = menu.buttons().getFirst();
        assertEquals(List.of(5), button.slots());
        assertEquals(Material.BARRIER, button.icon().material());
        assertEquals(ClickEffect.PRESS, button.effect());
        assertEquals(List.of(new Action.Message("ok")), button.actionsFor(ClickKind.ANY));
    }

    @Test
    void buttonWithoutSlotsIsDropped() throws InvalidConfigurationException {
        assertNull(load("buttons:\n  ghost:\n    material: stone\n").buttonAt(0));
    }
}
