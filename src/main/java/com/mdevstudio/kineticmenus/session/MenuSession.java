package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.animation.ClickEffect;
import com.mdevstudio.kineticmenus.animation.ClickEffect.Look;
import com.mdevstudio.kineticmenus.animation.OpenEffect;
import com.mdevstudio.kineticmenus.menu.Button;
import com.mdevstudio.kineticmenus.menu.Button.ClickKind;
import com.mdevstudio.kineticmenus.menu.Menu;
import com.mdevstudio.kineticmenus.render.IconRenderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * One open menu for one player. Everything visible is recomposed every tick from the base layout, the
 * opening animation and running click effects; only slots whose item actually changed are sent to the client.
 * Effects move to their next frame only on ticks, so a click shows the first frame at once and the effect keeps
 * the same pace however fast the player clicks.
 */
public final class MenuSession implements InventoryHolder {

    private static final int CLICK_COOLDOWN_TICKS = 3;
    private static final double PHASE_PER_TICK = 0.05;

    private final Menu menu;
    private final Player viewer;
    private final IconRenderer renderer;
    private final ActionRunner actions;
    private final Inventory inventory;
    private final ItemStack[] shown;
    private final Map<Integer, ItemStack> base = new HashMap<>();
    private final Set<Integer> freeSlots = new HashSet<>();
    private final List<RunningEffect> effects = new ArrayList<>();
    private final boolean live;

    private ItemStack filler;
    private List<Map<Integer, ItemStack>> opening;
    private int openingFrame;
    private long age;
    private long nextClick;

    MenuSession(Menu menu, Player viewer, IconRenderer renderer, ActionRunner actions, OpenEffect effect) {
        this.menu = menu;
        this.viewer = viewer;
        this.renderer = renderer;
        this.actions = actions;
        this.inventory = Bukkit.createInventory(this, menu.grid().size(), renderer.text(menu.title(), viewer, 0));
        this.shown = new ItemStack[menu.grid().size()];
        this.live = menu.buttons().stream().anyMatch(button -> button.icon().live())
                || menu.filler() != null && menu.filler().live();

        for (int slot = 0; slot < menu.grid().size(); slot++) {
            if (menu.buttonAt(slot) == null) {
                freeSlots.add(slot);
            }
        }
        renderBase();
        List<Map<Integer, ItemStack>> frames = effect.frames(base, menu.grid(), menu.openTicks(),
                ThreadLocalRandom.current());
        opening = frames.size() > 1 ? frames : null;
        draw(opening != null ? opening.getFirst() : base);
        openingFrame = 1;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Menu menu() {
        return menu;
    }

    public Player viewer() {
        return viewer;
    }

    void start() {
        viewer.openInventory(inventory);
        if (menu.openSound() != null) {
            viewer.playSound(Sound.sound(menu.openSound(), Sound.Source.MASTER, 0.6f, 1f));
        }
    }

    void tick() {
        age++;
        if (opening != null) {
            if (openingFrame < opening.size()) {
                draw(opening.get(openingFrame++));
                return;
            }
            opening = null;
        }
        if (live && age % menu.refreshTicks() == 0) {
            renderBase();
        }
        effects.removeIf(effect -> ++effect.frame >= effect.frames.size());
        draw(compose());
    }

    void click(int slot, ClickType type) {
        if (opening != null) {
            // An impatient click skips the rest of the opening instead of hitting a button that is still moving.
            opening = null;
            draw(compose());
            return;
        }
        if (age < nextClick) {
            return;
        }
        nextClick = age + CLICK_COOLDOWN_TICKS;

        Button button = menu.buttonAt(slot);
        if (button == null) {
            return;
        }
        if (button.permission() != null && !viewer.hasPermission(button.permission())) {
            play(button.denyEffect(), button, slot);
            actions.run(button.denyActions(), this);
            return;
        }
        play(button.effect(), button, slot);
        actions.run(button.actionsFor(kind(type)), this);
    }

    void refresh() {
        renderBase();
        draw(compose());
    }

    private void play(ClickEffect effect, Button button, int slot) {
        List<Map<Integer, Look>> frames = effect.frames(menu.grid(), slot, freeSlots);
        if (frames.isEmpty()) {
            return;
        }
        effects.removeIf(running -> running.slot == slot);
        ItemStack normal = base.get(slot);
        effects.add(new RunningEffect(slot, frames, Map.of(
                Look.BUTTON, normal,
                Look.PRESSED, renderer.render(button.pressed(), viewer, phase()),
                Look.GLINT, renderer.render(button.icon().withGlint(), viewer, phase()),
                Look.RIPPLE, renderer.render(menu.ripple(), viewer, phase()))));
        draw(compose());
    }

    private Map<Integer, ItemStack> compose() {
        Map<Integer, ItemStack> view = new HashMap<>(base);
        for (RunningEffect effect : effects) {
            effect.frames.get(effect.frame)
                    .forEach((slot, look) -> view.put(slot, look == Look.HIDDEN ? filler : effect.looks.get(look)));
        }
        return view;
    }

    private void renderBase() {
        double phase = phase();
        base.clear();
        filler = menu.filler() != null ? renderer.render(menu.filler(), viewer, phase) : null;
        if (filler != null) {
            freeSlots.forEach(slot -> base.put(slot, filler));
        }
        for (Button button : menu.buttons()) {
            ItemStack item = renderer.render(button.icon(), viewer, phase);
            button.slots().forEach(slot -> base.put(slot, item));
        }
    }

    // Compares references, not contents: unchanged slots keep the very same ItemStack, so this is cheap.
    private void draw(Map<Integer, ItemStack> view) {
        for (int slot = 0; slot < shown.length; slot++) {
            ItemStack item = view.get(slot);
            if (item != shown[slot]) {
                inventory.setItem(slot, item);
                shown[slot] = item;
            }
        }
    }

    private double phase() {
        double phase = (age * PHASE_PER_TICK) % 2;
        return phase - 1;
    }

    private static ClickKind kind(ClickType type) {
        return switch (type) {
            case LEFT -> ClickKind.LEFT;
            case RIGHT -> ClickKind.RIGHT;
            case SHIFT_LEFT -> ClickKind.SHIFT_LEFT;
            case SHIFT_RIGHT -> ClickKind.SHIFT_RIGHT;
            default -> ClickKind.ANY;
        };
    }

    private static final class RunningEffect {

        private final int slot;
        private final List<Map<Integer, Look>> frames;
        private final Map<Look, ItemStack> looks;
        private int frame;

        private RunningEffect(int slot, List<Map<Integer, Look>> frames, Map<Look, ItemStack> looks) {
            this.slot = slot;
            this.frames = frames;
            this.looks = looks;
        }
    }
}
