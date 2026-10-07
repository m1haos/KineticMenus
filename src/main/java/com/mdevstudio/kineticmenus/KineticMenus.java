package com.mdevstudio.kineticmenus;

import com.mdevstudio.kineticmenus.config.Messages;
import com.mdevstudio.kineticmenus.config.PluginConfig;
import com.mdevstudio.kineticmenus.menu.Menu;
import com.mdevstudio.kineticmenus.menu.MenuLoader;
import com.mdevstudio.kineticmenus.render.IconRenderer;
import com.mdevstudio.kineticmenus.session.SessionListener;
import com.mdevstudio.kineticmenus.session.Sessions;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.File;
import java.util.Map;
import org.bukkit.plugin.java.JavaPlugin;

public class KineticMenus extends JavaPlugin {

    private static final String MENUS_FOLDER = "menus";

    private PluginConfig settings;
    private Messages messages;
    private Map<String, Menu> menus = Map.of();
    private IconRenderer renderer;
    private Sessions sessions;

    @Override
    public void onEnable() {
        sessions = new Sessions(this);
        reload();
        getServer().getPluginManager().registerEvents(new SessionListener(sessions), this);
        sessions.start();
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> KineticMenusCommand.register(event.registrar(), this));
        getSLF4JLogger().info("Loaded {} menus", menus.size());
    }

    @Override
    public void onDisable() {
        if (sessions != null) {
            sessions.stop();
        }
    }

    /**
     * Reloads settings, messages and menus. Open menus are closed: they would keep showing the old layout.
     */
    public void reload() {
        settings = PluginConfig.load(this);
        messages = Messages.load(this, settings.language());

        File folder = new File(getDataFolder(), MENUS_FOLDER);
        if (!folder.isDirectory()) {
            saveResource(MENUS_FOLDER + "/showcase.yml", false);
        }
        renderer = new IconRenderer(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"));
        sessions.closeAll();
        menus = Map.copyOf(new MenuLoader(getSLF4JLogger()).loadAll(folder));
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

    public Sessions sessions() {
        return sessions;
    }
}
