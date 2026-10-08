package com.mdevstudio.kineticmenus.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.animation.Grid;
import com.mdevstudio.kineticmenus.menu.Action;
import com.mdevstudio.kineticmenus.menu.Button;
import com.mdevstudio.kineticmenus.menu.Button.ClickKind;
import com.mdevstudio.kineticmenus.menu.Condition;
import com.mdevstudio.kineticmenus.menu.Condition.Operator;
import com.mdevstudio.kineticmenus.menu.Menu;
import com.mdevstudio.kineticmenus.menu.MenuLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.slf4j.LoggerFactory;

class DeluxeMenusImportTest {

    private static final String SHOP = """
            menu_title: '&6&lShop &8» &7%player_name%'
            open_command: [shop, store]
            size: 27
            update_interval: 1
            open_requirement:
              requirements:
                perm:
                  type: has permission
                  permission: shop.use
              deny_commands: ['[message] &cNo access']
            items:
              bread:
                material: BREAD
                slot: 11
                priority: 2
                display_name: '&eBread'
                lore: ['&7Price: &#F2C14E20']
                enchantments: ['DURABILITY;1']
                view_requirement:
                  requirements:
                    level:
                      type: '>='
                      input: '%player_level%'
                      output: '5'
                left_click_requirement:
                  requirements:
                    money:
                      type: has money
                      amount: 20
                  deny_commands: ['[message] &cNot enough money', '[sound] ENTITY_VILLAGER_NO']
                left_click_commands:
                  - '[takemoney] 20'
                  - '[console] give %player_name% bread 16'
                  - '[sound] ENTITY_EXPERIENCE_ORB_PICKUP 1 2'
                  - '[openguimenu] tools <delay=10>'
                  - '[connect] lobby'
              head:
                material: basehead-eyJ0ZXh0dXJlcyI6e319
                slots: [0-2]
              hole:
                material: AIR
                slot: 13
            """;

    private final DeluxeMenusImport importer = new DeluxeMenusImport(LoggerFactory.getLogger("test"),
            name -> Map.of("ENTITY_VILLAGER_NO", "entity.villager.no",
                    "ENTITY_EXPERIENCE_ORB_PICKUP", "entity.experience_orb.pickup").get(name));

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void menuIsCarriedOverWithEverythingThatHasACounterpart(@TempDir Path plugins) throws IOException {
        Path deluxe = plugins.resolve("DeluxeMenus");
        Files.createDirectories(deluxe.resolve("gui_menus"));
        Files.writeString(deluxe.resolve("config.yml"), "gui_menus:\n  Shop:\n    file: shop.yml\n");
        Files.writeString(deluxe.resolve("gui_menus/shop.yml"), SHOP);
        Path target = plugins.resolve("KineticMenus/menus/imported");

        DeluxeMenusImport.Report report = importer.run(deluxe, target);

        assertEquals(List.of("shop"), report.imported());
        // head texture, air button, delay, unknown [connect] action
        assertEquals(4, report.warnings());
        Menu menu = new MenuLoader(LoggerFactory.getLogger("test")).loadAll(target).get("shop");

        assertEquals(new Grid(3), menu.grid());
        assertEquals(List.of("shop", "store"), menu.commands());
        assertEquals(20, menu.refreshTicks());
        assertEquals(List.of(new Condition.Permission("shop.use")), menu.openIf());
        assertTrue(menu.title().contains("<gold>") && menu.title().contains("%player_name%"));

        Button bread = menu.buttons().stream().filter(button -> button.id().equals("bread")).findFirst().orElseThrow();
        assertEquals(List.of(11), bread.slots());
        assertEquals(2, bread.priority());
        assertTrue(bread.icon().glint());
        assertEquals("<gray>Price: </gray><#F2C14E>20", bread.icon().lore().getFirst());
        assertEquals(List.of(new Condition.Compare("%player_level%", Operator.AT_LEAST, "5")), bread.showIf());
        assertEquals(List.of(new Condition.Money(20)), bread.clickIf());
        assertEquals(2, bread.deny().actions().size());

        List<Action> click = bread.actionsFor(ClickKind.LEFT);
        assertEquals(new Action.TakeMoney(20), click.get(0));
        assertEquals(new Action.ConsoleCommand("give %player_name% bread 16"), click.get(1));
        assertTrue(click.get(2) instanceof Action.Sound sound && sound.pitch() == 2);
        assertEquals(new Action.Open("tools", null), click.get(3));
        assertEquals(4, click.size());

        Button head = menu.buttons().stream().filter(button -> button.id().equals("head")).findFirst().orElseThrow();
        assertEquals(Material.PLAYER_HEAD, head.icon().material());
        assertEquals(List.of(0, 1, 2), head.slots());
        assertFalse(menu.buttons().stream().anyMatch(button -> button.id().equals("hole")));
    }

    @Test
    void existingFilesAreLeftAlone(@TempDir Path plugins) throws IOException {
        Path deluxe = plugins.resolve("DeluxeMenus");
        Files.createDirectories(deluxe.resolve("gui_menus"));
        Files.writeString(deluxe.resolve("gui_menus/shop.yml"), SHOP);
        Path target = plugins.resolve("menus");
        Files.createDirectories(target);
        Files.writeString(target.resolve("shop.yml"), "title: mine\n");

        DeluxeMenusImport.Report report = importer.run(deluxe, target);

        assertEquals(List.of("shop"), report.skipped());
        assertEquals("title: mine\n", Files.readString(target.resolve("shop.yml")));
    }
}
