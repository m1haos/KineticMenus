package com.mdevstudio.kineticmenus.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.animation.AmbientEffect;
import com.mdevstudio.kineticmenus.animation.ClickEffect;
import com.mdevstudio.kineticmenus.animation.Easing;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.animation.SoundCue;
import com.mdevstudio.kineticmenus.menu.Button.ClickKind;
import com.mdevstudio.kineticmenus.menu.Condition.Operator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
                  effect: Slide-Left
                  ticks: 9
                  easing: ease-out
                ambient:
                  effect: twinkle
                  speed: 4
                buttons:
                  row:
                    slots: ["9-11", 13]
                    material: diamond
                    effect: ripple
                    on-click: ["message: hi", "sound: ui.button.click 0.5 2", "open: other scatter"]
                    on-right-click: ["close", "take-money: 15.5"]
                """);

        assertEquals(new Menu.Opening(OpenEffect.SLIDE_LEFT, 9, Easing.EASE_OUT, null), menu.opening());
        assertEquals(new Menu.Ambient(AmbientEffect.TWINKLE, 4), menu.ambient());
        Button button = menu.buttons().getFirst();
        assertEquals(List.of(9, 10, 11, 13), button.slots());
        assertEquals(ClickEffect.RIPPLE, button.effect());
        assertEquals(new Action.Open("other", OpenEffect.SCATTER), button.actionsFor(ClickKind.ANY).get(2));
        assertEquals(List.of(new Action.Close(), new Action.TakeMoney(15.5)), button.actionsFor(ClickKind.RIGHT));
        assertEquals(3, button.actionsFor(ClickKind.LEFT).size());
    }

    @Test
    void readsConditionsPrioritiesAndCommands() throws InvalidConfigurationException {
        Menu menu = load("""
                command: /Shop
                commands: [store, shop]
                permission: shop.use
                open-if: ["!placeholder: %player_world% == nether"]
                buttons:
                  locked:
                    slot: 1
                    priority: 5
                  vip:
                    slot: 1
                    priority: 1
                    permission: shop.vip
                    show-if: ["money: 10", "placeholder: %player_level%>=30"]
                """);

        assertEquals(List.of("shop", "store"), menu.commands());
        assertEquals(List.of(new Condition.Permission("shop.use"), new Condition.Not(
                new Condition.Compare("%player_world%", Operator.EQUAL, "nether"))), menu.openIf());
        Button vip = menu.buttons().getFirst();
        assertEquals("vip", vip.id());
        assertEquals(List.of(new Condition.Money(10),
                new Condition.Compare("%player_level%", Operator.AT_LEAST, "30")), vip.showIf());
        assertEquals(List.of(new Condition.Permission("shop.vip")), vip.clickIf());
    }

    @Test
    void readsSoundChoices() throws InvalidConfigurationException {
        Menu menu = load("""
                open:
                  sound: none
                buttons:
                  auto:
                    slot: 0
                  custom:
                    slot: 1
                    sound: ["block.note_block.pling 0.5 1.5", "minecraft:ui.button.click 1 1 4"]
                    deny-sound: none
                """);

        assertEquals(List.of(), menu.opening().sound());
        assertNull(menu.buttons().getFirst().sound());
        Button custom = menu.buttons().get(1);
        assertEquals(List.of(new SoundCue(0, Key.key("block.note_block.pling"), 0.5f, 1.5f),
                new SoundCue(4, Key.key("ui.button.click"), 1, 1)), custom.sound());
        assertEquals(List.of(), custom.deny().sound());
    }

    @Test
    void mistakesAreSkippedInsteadOfBreakingTheMenu() throws InvalidConfigurationException {
        Menu menu = load("""
                rows: 9
                open:
                  effect: explode
                ambient:
                  effect: disco
                buttons:
                  broken:
                    slots: [5, 99, "x"]
                    material: not_a_block
                    effect: wobble
                    show-if: ["level: 5", "money: lots", "placeholder: no comparison here"]
                    on-click: ["teleport: spawn", "take-money: -5", "message: ok"]
                  outside:
                    slot: 70
                """);

        assertEquals(3, menu.grid().rows());
        assertEquals(OpenEffect.NONE, menu.opening().effect());
        assertEquals(AmbientEffect.NONE, menu.ambient().effect());
        assertEquals(1, menu.buttons().size());
        Button button = menu.buttons().getFirst();
        assertEquals(List.of(5), button.slots());
        assertEquals(Material.BARRIER, button.icon().material());
        assertEquals(ClickEffect.PRESS, button.effect());
        assertTrue(button.showIf().isEmpty());
        assertEquals(List.of(new Action.Message("ok")), button.actionsFor(ClickKind.ANY));
    }

    @Test
    void buttonWithoutSlotsIsDropped() throws InvalidConfigurationException {
        assertTrue(load("buttons:\n  ghost:\n    material: stone\n").buttons().isEmpty());
    }

    @Test
    void menusInSubfoldersLoadAndDuplicateIdsAreSkipped(@TempDir Path folder) throws IOException {
        Files.createDirectories(folder.resolve("shop"));
        Files.writeString(folder.resolve("main.yml"), "title: first\n");
        Files.writeString(folder.resolve("shop/page.yml"), "title: page\n");
        Files.writeString(folder.resolve("shop/main.yml"), "title: second\n");

        var menus = loader.loadAll(folder);

        assertEquals(2, menus.size());
        assertEquals("first", menus.get("main").title());
        assertEquals("page", menus.get("page").title());
    }
}
