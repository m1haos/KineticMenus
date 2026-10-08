package com.mdevstudio.kineticmenus;

import com.mdevstudio.kineticmenus.config.Messages;
import com.mdevstudio.kineticmenus.config.PluginConfig;
import com.mdevstudio.kineticmenus.hook.BedrockPlayers;
import com.mdevstudio.kineticmenus.hook.VaultWallet;
import com.mdevstudio.kineticmenus.hook.Wallet;
import com.mdevstudio.kineticmenus.menu.Menu;
import com.mdevstudio.kineticmenus.menu.MenuLoader;
import com.mdevstudio.kineticmenus.render.IconRenderer;
import com.mdevstudio.kineticmenus.session.SessionListener;
import com.mdevstudio.kineticmenus.session.Sessions;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class KineticMenus extends JavaPlugin {

    private static final String MENUS_FOLDER = "menus";
    private static final List<String> BUNDLED_MENUS =
            List.of("showcase.yml", "navigator.yml", "shop.yml", "shop-tools.yml");

    private PluginConfig settings;
    private Messages messages;
    private Map<String, Menu> menus = Map.of();
    private IconRenderer renderer;
    private Wallet wallet = Wallet.NONE;
    private BedrockPlayers bedrockPlayers;
    private Sessions sessions;
    private MenuWatcher watcher;
    private Set<String> menuCommands = Set.of();

    @Override
    public void onEnable() {
        sessions = new Sessions(this);
        reload();
        getServer().getPluginManager().registerEvents(new SessionListener(sessions), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> menuCommands = KineticMenusCommand.register(event.registrar(), this));
        getSLF4JLogger().info("Loaded {} menus", menus.size());
    }

    @Override
    public void onDisable() {
        if (watcher != null) {
            watcher.stop();
        }
        if (sessions != null) {
            sessions.closeAll();
        }
    }

    /**
     * Reloads settings, messages and menus. Open menus are redrawn from the new files.
     */
    public void reload() {
        settings = PluginConfig.load(this);
        messages = Messages.load(this, settings.language());

        Path folder = getDataPath().resolve(MENUS_FOLDER);
        if (!Files.isDirectory(folder)) {
            BUNDLED_MENUS.forEach(file -> saveResource(MENUS_FOLDER + "/" + file, false));
        }
        PluginManager plugins = getServer().getPluginManager();
        renderer = new IconRenderer(plugins.isPluginEnabled("PlaceholderAPI"));
        if (plugins.getPlugin("Vault") != null) {
            wallet = new VaultWallet(getServer());
        }
        bedrockPlayers = new BedrockPlayers(plugins.isPluginEnabled("floodgate"));
        menus = Map.copyOf(new MenuLoader(getSLF4JLogger()).loadAll(folder));
        sessions.reopenAll();

        if (settings.autoReload() && watcher == null) {
            watcher = new MenuWatcher(folder, getSLF4JLogger(), () -> getServer().getGlobalRegionScheduler()
                    .execute(this, this::reloadAfterEdit));
            watcher.start();
        } else if (!settings.autoReload() && watcher != null) {
            watcher.stop();
            watcher = null;
        }
    }

    /**
     * Commands of loaded menus that are not registered yet: the server only takes new commands on start.
     */
    public List<String> unregisteredCommands() {
        return menus.values().stream().flatMap(menu -> menu.commands().stream())
                .filter(command -> !menuCommands.contains(command)).sorted().toList();
    }

    private void reloadAfterEdit() {
        reload();
        getSLF4JLogger().info("Menu files changed, reloaded {} menus", menus.size());
        List<String> waiting = unregisteredCommands();
        if (!waiting.isEmpty()) {
            getSLF4JLogger().warn("New menu commands {} will work after a server restart", waiting);
        }
    }

    public PluginConfig settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public Map<String, Menu> menus() {
        return menus;
    }

    public IconRenderer renderer() {
        return renderer;
    }

    public Wallet wallet() {
        return wallet;
    }

    public BedrockPlayers bedrockPlayers() {
        return bedrockPlayers;
    }

    public Sessions sessions() {
        return sessions;
    }

    // Tests have no Vault, so they hand in a wallet of their own.
    void useWallet(Wallet wallet) {
        this.wallet = wallet;
    }
}
