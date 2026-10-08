package com.mdevstudio.kineticmenus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.hook.Wallet;
import com.mdevstudio.kineticmenus.session.MenuSession;
import com.mdevstudio.kineticmenus.session.Sessions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
    private static final int OPEN_CASCADE = 10;
    private static final int SLIDE = 22;
    private static final int PRESS = 28;
    private static final int RIPPLE = 30;
    private static final int SHAKE = 33;
    private static final int CLOSE = 49;

    private static final int SHOP_BREAD = 20;
    private static final int SHOP_CRATE = 24;

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
    void bundledMenusAreCreatedOnFirstStart() {
        assertTrue(plugin.menus().keySet().containsAll(List.of("showcase", "navigator", "shop",
                "shop-tools")));
        assertTrue(Files.exists(plugin.getDataPath().resolve("menus/shop.yml")));
    }

    @Test
    void openingAnimationEndsWithEveryItemInPlace() {
        plugin.sessions().open(player, "showcase", OpenEffect.SCATTER);
        server.getScheduler().performTicks(20);

        Inventory menu = top();
        assertEquals(Material.NETHER_STAR, menu.getItem(LOGO).getType());
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, menu.getItem(9).getType());
        assertEquals(Material.BARRIER, menu.getItem(CLOSE).getType());
    }

    @Test
    void cascadeStartsAtTheTopLeft() {
        plugin.sessions().open(player, "showcase", OpenEffect.CASCADE);

        assertEquals(Material.BLACK_STAINED_GLASS_PANE, top().getItem(0).getType());
        assertNull(top().getItem(CLOSE));
        server.getScheduler().performTicks(3);
        assertEquals(Material.NETHER_STAR, top().getItem(LOGO).getType());
    }

    @Test
    void openingPlaysTheSoundsOfItsEffect() {
        plugin.sessions().open(player, "showcase", OpenEffect.TYPEWRITER);
        server.getScheduler().performTicks(20);

        assertTrue(heard("block.note_block.hat"));
        assertTrue(heard("block.note_block.bell"));
    }

    @Test
    void playersWhoMutedMenusHearNothing() {
        plugin.sessions().setMuted(player, true);

        plugin.sessions().open(player, "showcase", OpenEffect.TYPEWRITER);
        server.getScheduler().performTicks(20);
        click(PRESS);

        assertTrue(player.getHeardSounds().isEmpty());
    }

    @Test
    void playersWithAnimationsOffSeeTheMenuAtOnceButStillHearClicks() {
        plugin.sessions().setReducedMotion(player, true);

        plugin.sessions().open(player, "showcase", OpenEffect.SCATTER);
        assertEquals(Material.BARRIER, top().getItem(CLOSE).getType());

        server.getScheduler().performTicks(4);
        click(PRESS);
        assertFalse(glint(top().getItem(PRESS)));
        assertTrue(heard("ui.button.click"));
    }

    @Test
    void bedrockPlayersSeeTheMenuAtOnce() {
        PlayerMock bedrock = new PlayerMock(server, "BedrockSteve", new UUID(0, 42));
        server.addPlayer(bedrock);

        plugin.sessions().open(bedrock, "showcase", OpenEffect.SCATTER);

        assertEquals(Material.BARRIER, bedrock.getOpenInventory().getTopInventory().getItem(CLOSE).getType());
    }

    @Test
    void clickDuringTheOpeningSkipsIt() {
        plugin.sessions().open(player, "showcase", OpenEffect.TYPEWRITER);

        click(LOGO);

        assertEquals(Material.BARRIER, top().getItem(CLOSE).getType());
    }

    @Test
    void pressAddsGlintForAMomentThenReturnsTheButton() {
        openSettled("showcase");

        click(PRESS);
        assertTrue(glint(top().getItem(PRESS)));

        server.getScheduler().performTicks(5);
        assertFalse(glint(top().getItem(PRESS)));
    }

    @Test
    void rippleSpreadsOverEmptySlotsAndFades() {
        openSettled("showcase");

        click(RIPPLE);

        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, top().getItem(21).getType());
        // Slot 29 is on the first ring too, but it is a button and must stay untouched.
        assertEquals(Material.GLOWSTONE_DUST, top().getItem(29).getType());
        server.getScheduler().performOneTick();
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, top().getItem(21).getType());
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, top().getItem(19).getType());
        server.getScheduler().performTicks(10);
        assertEquals(Material.BLACK_STAINED_GLASS_PANE, top().getItem(21).getType());
    }

    @Test
    void borderLightsRunAroundWhileTheMenuIsOpen() {
        openSettled("showcase");

        boolean lit = false;
        for (int tick = 0; tick < 60 && !lit; tick++) {
            server.getScheduler().performOneTick();
            lit = top().getItem(0).getType() == Material.LIGHT_BLUE_STAINED_GLASS_PANE;
        }
        assertTrue(lit);
    }

    @Test
    void buttonWithUnmetClickConditionRefusesAndRunsItsDenyActions() {
        openSettled("showcase");
        drainMessages();

        click(SHAKE);

        assertTrue(drainMessages().contains("shakes its head"));
        assertTrue(heard("block.note_block.bass"));
    }

    @Test
    void openActionSwitchesToANewMenuOnTheNextTick() {
        openSettled("showcase");
        MenuSession before = session();

        click(OPEN_CASCADE);
        server.getScheduler().performOneTick();

        assertNotSame(before, session());
    }

    @Test
    void slideStartsFromWhatThePlayerSawBefore() {
        openSettled("showcase");

        click(SLIDE);
        server.getScheduler().performOneTick();

        assertEquals("navigator", session().menu().id());
        // The old logo has moved one column to the left.
        assertEquals(Material.NETHER_STAR, top().getItem(LOGO - 1).getType());
        server.getScheduler().performTicks(20);
        assertEquals(Material.PLAYER_HEAD, top().getItem(LOGO).getType());
    }

    @Test
    void closeActionClosesTheMenu() {
        openSettled("showcase");

        click(CLOSE);
        server.getScheduler().performOneTick();

        Inventory top = top();
        assertFalse(top != null && top.getHolder(false) instanceof MenuSession);
        assertEquals(0, plugin.sessions().openCount());
    }

    @Test
    void itemsCannotBeTakenOutOfTheMenu() {
        openSettled("showcase");

        assertTrue(click(PRESS).isCancelled());
    }

    @Test
    void reloadRedrawsOpenMenusInPlace() {
        openSettled("showcase");
        MenuSession before = session();

        plugin.reload();
        server.getScheduler().performOneTick();

        assertNotSame(before, session());
        assertEquals("showcase", session().menu().id());
        assertEquals(1, plugin.sessions().openCount());
    }

    @Test
    void buyingTakesTheMoneyOnce() {
        TestWallet wallet = new TestWallet(100);
        plugin.useWallet(wallet);
        openSettled("shop");
        drainMessages();

        click(SHOP_BREAD);

        assertEquals(80, wallet.balance);
        assertTrue(drainMessages().contains("Bought 16 bread"));
        assertTrue(heard("entity.experience_orb.pickup"));
    }

    @Test
    void buyingWithoutEnoughMoneyIsRefused() {
        TestWallet wallet = new TestWallet(5);
        plugin.useWallet(wallet);
        openSettled("shop");
        drainMessages();

        click(SHOP_BREAD);

        assertEquals(5, wallet.balance);
        assertTrue(drainMessages().contains("You need 20"));
    }

    @Test
    void buttonsSharingASlotShowTheFirstOneWhoseConditionsAreMet() {
        openSettled("shop");
        assertEquals(Material.CHEST, top().getItem(SHOP_CRATE).getType());

        player.addAttachment(plugin, "shop.vip", true);
        server.getScheduler().performTicks(3);

        assertEquals(Material.ENDER_CHEST, top().getItem(SHOP_CRATE).getType());
    }

    @Test
    void menuWithUnmetOpenConditionStaysClosed() throws IOException {
        Files.writeString(plugin.getDataPath().resolve("menus/secret.yml"), "permission: secret.open\n");
        plugin.reload();
        drainMessages();

        assertEquals(Sessions.Result.DENIED, plugin.sessions().open(player, "secret", null));
        assertTrue(drainMessages().contains("cannot open"));
    }

    @Test
    void savedMenuFilesAreLoadedWithoutAReloadCommand() throws IOException, InterruptedException {
        Path file = plugin.getDataPath().resolve("menus/fresh.yml");
        Files.writeString(file, "title: Fresh\n");

        for (int attempt = 0; attempt < 100 && !plugin.menus().containsKey("fresh"); attempt++) {
            Thread.sleep(50);
            server.getScheduler().performOneTick();
        }

        assertTrue(plugin.menus().containsKey("fresh"));
    }

    private void openSettled(String menu) {
        plugin.sessions().open(player, menu, OpenEffect.NONE);
        server.getScheduler().performTicks(4);
        player.clearSounds();
    }

    private Inventory top() {
        return player.getOpenInventory().getTopInventory();
    }

    private MenuSession session() {
        MenuSession session = (MenuSession) top().getHolder(false);
        assertSame(player, session.viewer());
        return session;
    }

    private InventoryClickEvent click(int slot) {
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), SlotType.CONTAINER, slot,
                ClickType.LEFT, InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private boolean heard(String sound) {
        return player.getHeardSounds().stream().anyMatch(heard -> heard.getSound().endsWith(sound));
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

    private static final class TestWallet implements Wallet {

        private double balance;

        private TestWallet(double balance) {
            this.balance = balance;
        }

        @Override
        public boolean has(Player player, double amount) {
            return balance >= amount;
        }

        @Override
        public boolean take(Player player, double amount) {
            if (balance < amount) {
                return false;
            }
            balance -= amount;
            return true;
        }

        @Override
        public void give(Player player, double amount) {
            balance += amount;
        }
    }
}
