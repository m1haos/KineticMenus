package com.mdevstudio.kineticmenus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.session.MenuSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

class KineticMenusTest {

    private static final int LOGO = 4;
    private static final int PRESS = 28;
    private static final int RIPPLE = 31;
    private static final int SHAKE = 33;
    private static final int CLOSE = 40;
    private static final int OPEN_CASCADE = 10;

    private ServerMock server;
    private KineticMenus plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock(new PaperServerMock());
        plugin = MockBukkit.load(KineticMenus.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void showcaseMenuIsCreatedOnFirstStart() {
        assertTrue(plugin.menus().containsKey("showcase"));
        assertTrue(plugin.getDataFolder().toPath().resolve("menus/showcase.yml").toFile().exists());
    }

    @Test
    void openingAnimationEndsWithEveryItemInPlace() {
        plugin.sessions().open(player, "showcase", OpenEffect.SCATTER);
        server.getScheduler().performTicks(20);

        Inventory menu = top();
        assertEquals(Material.NETHER_STAR, menu.getItem(LOGO).getType());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getItem(0).getType());
        assertEquals(Material.BARRIER, menu.getItem(CLOSE).getType());
    }

    @Test
    void cascadeStartsWithTheTopRowOnly() {
        plugin.sessions().open(player, "showcase", OpenEffect.CASCADE);

        assertEquals(Material.NETHER_STAR, top().getItem(LOGO).getType());
        assertNull(top().getItem(CLOSE));
    }

    @Test
    void playersWithAnimationsOffSeeTheMenuAtOnce() {
        plugin.sessions().setReducedMotion(player, true);

        plugin.sessions().open(player, "showcase", OpenEffect.SCATTER);

        assertEquals(Material.BARRIER, top().getItem(CLOSE).getType());
    }

    @Test
    void clickDuringTheOpeningSkipsIt() {
        plugin.sessions().open(player, "showcase", OpenEffect.TYPEWRITER);

        click(LOGO);

        assertEquals(Material.BARRIER, top().getItem(CLOSE).getType());
    }

    @Test
    void pressAddsGlintForAMomentThenReturnsTheButton() {
        openSettled();

        click(PRESS);
        assertTrue(glint(top().getItem(PRESS)));

        server.getScheduler().performTicks(5);
        assertFalse(glint(top().getItem(PRESS)));
    }

    @Test
    void rippleColoursEmptySlotsAroundTheButton() {
        openSettled();

        click(RIPPLE);

        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, top().getItem(RIPPLE - 1).getType());
        server.getScheduler().performOneTick();
        // Second ring: slot 38 is empty, while slot 29 on the same ring is a button and must stay untouched.
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, top().getItem(38).getType());
        assertEquals(Material.GLOWSTONE_DUST, top().getItem(29).getType());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, top().getItem(RIPPLE - 1).getType());
        server.getScheduler().performTicks(8);
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, top().getItem(RIPPLE - 1).getType());
    }

    @Test
    void buttonWithoutPermissionRefusesAndRunsItsDenyActions() {
        openSettled();
        drainMessages();

        click(SHAKE);

        assertTrue(drainMessages().contains("shakes its head"));
    }

    @Test
    void openActionSwitchesToANewMenuOnTheNextTick() {
        openSettled();
        MenuSession before = (MenuSession) top().getHolder(false);

        click(OPEN_CASCADE);
        server.getScheduler().performOneTick();

        assertNotSame(before, top().getHolder(false));
        assertTrue(top().getHolder(false) instanceof MenuSession);
    }

    @Test
    void closeActionClosesTheMenu() {
        openSettled();

        click(CLOSE);
        server.getScheduler().performOneTick();

        Inventory top = top();
        assertFalse(top != null && top.getHolder(false) instanceof MenuSession);
        assertEquals(0, plugin.sessions().openCount());
    }

    @Test
    void itemsCannotBeTakenOutOfTheMenu() {
        openSettled();

        InventoryClickEvent event = click(PRESS);

        assertTrue(event.isCancelled());
    }

    @Test
    void reloadClosesOpenMenus() {
        openSettled();

        plugin.reload();

        assertEquals(0, plugin.sessions().openCount());
    }

    private void openSettled() {
        plugin.sessions().open(player, "showcase", OpenEffect.NONE);
        server.getScheduler().performTicks(4);
    }

    private Inventory top() {
        return player.getOpenInventory().getTopInventory();
    }

    private InventoryClickEvent click(int slot) {
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), SlotType.CONTAINER, slot,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private static boolean glint(ItemStack item) {
        return item.getItemMeta().hasEnchantmentGlintOverride() && item.getItemMeta().getEnchantmentGlintOverride();
    }

    private String drainMessages() {
        StringBuilder text = new StringBuilder();
        for (Component message = player.nextComponentMessage(); message != null;
                message = player.nextComponentMessage()) {
            text.append(PlainTextComponentSerializer.plainText().serialize(message)).append('\n');
        }
        return text.toString();
    }
}
