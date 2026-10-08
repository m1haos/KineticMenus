package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.animation.AmbientEffect;
import com.mdevstudio.kineticmenus.animation.Animation;
import com.mdevstudio.kineticmenus.animation.ClickEffect;
import com.mdevstudio.kineticmenus.animation.ClickEffect.Look;
import com.mdevstudio.kineticmenus.animation.Scene;
import com.mdevstudio.kineticmenus.animation.SoundCue;
import com.mdevstudio.kineticmenus.config.Messages;
import com.mdevstudio.kineticmenus.menu.Button;
import com.mdevstudio.kineticmenus.menu.Button.ClickKind;
import com.mdevstudio.kineticmenus.menu.Button.Reaction;
import com.mdevstudio.kineticmenus.menu.Icon;
import com.mdevstudio.kineticmenus.menu.Menu;
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
 * One open menu for one player. Everything visible is recomposed every tick from the base layout, the opening
 * animation, the background effect and running click effects; only slots whose item actually changed are sent to
 * the client. Effects move to their next frame only on ticks, so a click shows the first frame at once and the
 * effect keeps the same pace however fast the player clicks.
 */
public final class MenuSession implements InventoryHolder {

    private static final int CLICK_COOLDOWN_TICKS = 3;
    private static final double PHASE_PER_TICK = 0.05;

    private final Sessions sessions;
    private final Menu menu;
    private final Player viewer;
    private final Presentation presentation;
    private final Inventory inventory;
    private final ItemStack[] shown;
    private final Map<Integer, ItemStack> base = new HashMap<>();
    private final Map<Integer, Button> visible = new HashMap<>();
    private final Set<Integer> freeSlots = new HashSet<>();
    private final Map<Icon, ItemStack> still = new HashMap<>();
    private final List<RunningEffect> effects = new ArrayList<>();
    private final List<PendingSound> sounds = new ArrayList<>();
    private final boolean live;

    private ItemStack filler;
    private ItemStack accent;
    private ItemStack trail;
    private List<Map<Integer, ItemStack>> opening;
    private int openingFrame;
    private long age;
    private long nextClick;
    private boolean stale;
    private boolean stopped;

    MenuSession(Sessions sessions, Menu menu, Player viewer, Presentation presentation) {
        this.sessions = sessions;
        this.menu = menu;
        this.viewer = viewer;
        this.presentation = presentation;
        this.inventory = Bukkit.createInventory(this, menu.grid().size(),
                sessions.renderer().text(menu.title(), viewer, 0));
        this.shown = new ItemStack[menu.grid().size()];
        this.live = menu.buttons().stream().anyMatch(button -> button.icon().live() || !button.showIf().isEmpty())
                || menu.filler() != null && menu.filler().live();

        renderBase();
        Map<Integer, ItemStack> items = new HashMap<>();
        Map<Integer, ItemStack> background = new HashMap<>();
        base.forEach((slot, item) -> (visible.containsKey(slot) ? items : background).put(slot, item));
        Animation<ItemStack> animation = presentation.effect().animate(
                new Scene<>(menu.grid(), items, background, presentation.previous(), accent, trail),
                menu.opening().ticks(), menu.opening().easing(), ThreadLocalRandom.current());

        opening = animation.frames().size() > 1 ? animation.frames() : null;
        draw(animation.frames().getFirst());
        openingFrame = 1;
        List<SoundCue> openSound = menu.opening().sound();
        schedule(openSound != null ? openSound : animation.sounds(), true);
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

    /**
     * What the viewer sees right now.
     */
    public Map<Integer, ItemStack> view() {
        Map<Integer, ItemStack> view = new HashMap<>();
        for (int slot = 0; slot < shown.length; slot++) {
            if (shown[slot] != null) {
                view.put(slot, shown[slot]);
            }
        }
        return view;
    }

    void start() {
        viewer.openInventory(inventory);
        scheduleTick();
    }

    void stop() {
        stopped = true;
    }

    // Every tick books the next one, so a closed menu simply stops without a task to cancel. The viewer's own
    // scheduler runs it on the viewer's thread, which is what Folia needs.
    private void scheduleTick() {
        viewer.getScheduler().run(sessions.plugin(), task -> {
            if (!stopped) {
                scheduleTick();
                tick();
            }
        }, null);
    }

    void tick() {
        age++;
        if (opening != null) {
            if (openingFrame < opening.size()) {
                draw(opening.get(openingFrame++));
                playDue();
                return;
            }
            opening = null;
        }
        if (stale || live && age % menu.refreshTicks() == 0) {
            renderBase();
            stale = false;
        }
        effects.removeIf(effect -> ++effect.frame >= effect.frames.size());
        draw(compose());
        playDue();
    }

    void click(int slot, ClickType type) {
        if (opening != null) {
            // An impatient click skips the rest of the opening instead of hitting a button that is still moving.
            opening = null;
            sounds.removeIf(PendingSound::opening);
            draw(compose());
            return;
        }
        if (age < nextClick) {
            return;
        }
        nextClick = age + CLICK_COOLDOWN_TICKS;

        Button button = visible.get(slot);
        if (button == null) {
            return;
        }
        if (!sessions.requirements().test(button.clickIf(), viewer)) {
            deny(button, slot, null);
        } else if (sessions.actions().run(button.actionsFor(kind(type)), viewer, this)) {
            react(button.effect(), button.sound(), button, slot);
        } else {
            deny(button, slot, Messages.NOT_ENOUGH_MONEY);
        }
        // Whatever the click did may change which buttons are shown, so conditions are checked again next tick.
        stale = true;
    }

    void refresh() {
        renderBase();
        draw(compose());
    }

    // fallback is sent when the button has no deny actions of its own; null sends nothing.
    private void deny(Button button, int slot, String fallback) {
        Reaction deny = button.deny();
        react(deny.effect(), deny.sound(), button, slot);
        if (!deny.actions().isEmpty()) {
            sessions.actions().run(deny.actions(), viewer, this);
        } else if (fallback != null) {
            sessions.messages().send(viewer, fallback);
        }
    }

    private void react(ClickEffect effect, List<SoundCue> sound, Button button, int slot) {
        Animation<Look> animation = effect.animate(menu.grid(), slot, freeSlots, ThreadLocalRandom.current());
        schedule(sound != null ? sound : animation.sounds(), false);
        if (!presentation.motion() || animation.frames().isEmpty()) {
            return;
        }
        effects.removeIf(running -> running.slot == slot);
        Map<Look, ItemStack> looks = new HashMap<>();
        looks.put(Look.BUTTON, base.get(slot));
        looks.put(Look.PRESSED, item(button.pressed()));
        looks.put(Look.GLINT, item(button.icon().withGlint()));
        effects.add(new RunningEffect(slot, animation.frames(), looks));
        draw(compose());
    }

    private Map<Integer, ItemStack> compose() {
        Map<Integer, ItemStack> view = new HashMap<>(base);
        Menu.Ambient ambient = menu.ambient();
        if (presentation.motion() && ambient.effect() != AmbientEffect.NONE) {
            ambient.effect().frame(menu.grid(), freeSlots, age, ambient.speed())
                    .forEach((slot, look) -> view.put(slot, shared(look)));
        }
        for (RunningEffect effect : effects) {
            effect.frames.get(effect.frame).forEach((slot, look) -> {
                ItemStack own = effect.looks.get(look);
                view.put(slot, own != null ? own : shared(look));
            });
        }
        return view;
    }

    private ItemStack shared(Look look) {
        return switch (look) {
            case ACCENT -> accent;
            case TRAIL -> trail;
            default -> filler;
        };
    }

    private void renderBase() {
        base.clear();
        visible.clear();
        freeSlots.clear();
        filler = menu.filler() != null ? item(menu.filler()) : null;
        accent = item(menu.accent());
        trail = item(menu.trail());

        // Buttons come sorted by priority: the first one whose conditions are met takes the slot.
        for (Button button : menu.buttons()) {
            List<Integer> slots = button.slots().stream().filter(slot -> !visible.containsKey(slot)).toList();
            if (slots.isEmpty() || !sessions.requirements().test(button.showIf(), viewer)) {
                continue;
            }
            ItemStack item = item(button.icon());
            slots.forEach(slot -> {
                visible.put(slot, button);
                base.put(slot, item);
            });
        }
        for (int slot = 0; slot < menu.grid().size(); slot++) {
            if (!visible.containsKey(slot)) {
                freeSlots.add(slot);
                if (filler != null) {
                    base.put(slot, filler);
                }
            }
        }
    }

    // Icons without placeholders or gradients are rendered once: the same ItemStack keeps draw() from resending them.
    private ItemStack item(Icon icon) {
        return icon.live() ? sessions.renderer().render(icon, viewer, phase())
                : still.computeIfAbsent(icon, key -> sessions.renderer().render(key, viewer, 0));
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

    private void schedule(List<SoundCue> cues, boolean partOfOpening) {
        if (presentation.sound()) {
            cues.forEach(cue -> sounds.add(new PendingSound(age + cue.tick(), cue, partOfOpening)));
            playDue();
        }
    }

    private void playDue() {
        float volume = sessions.volume();
        sounds.removeIf(pending -> {
            if (pending.at() > age) {
                return false;
            }
            SoundCue cue = pending.cue();
            viewer.playSound(Sound.sound(cue.sound(), Sound.Source.UI, cue.volume() * volume, cue.pitch()));
            return true;
        });
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

    private record PendingSound(long at, SoundCue cue, boolean opening) {
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
