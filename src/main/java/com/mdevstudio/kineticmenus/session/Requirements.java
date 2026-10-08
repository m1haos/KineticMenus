package com.mdevstudio.kineticmenus.session;

import com.mdevstudio.kineticmenus.hook.Wallet;
import com.mdevstudio.kineticmenus.menu.Condition;
import com.mdevstudio.kineticmenus.menu.Condition.Operator;
import com.mdevstudio.kineticmenus.render.IconRenderer;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.entity.Player;

final class Requirements {

    private final Supplier<IconRenderer> renderer;
    private final Supplier<Wallet> wallet;

    Requirements(Supplier<IconRenderer> renderer, Supplier<Wallet> wallet) {
        this.renderer = renderer;
        this.wallet = wallet;
    }

    boolean test(List<Condition> conditions, Player player) {
        return conditions.stream().allMatch(condition -> test(condition, player));
    }

    private boolean test(Condition condition, Player player) {
        return switch (condition) {
            case Condition.Permission permission -> player.hasPermission(permission.node());
            case Condition.Money money -> wallet.get().has(player, money.amount());
            case Condition.Compare compare -> compare(renderer.get().fill(compare.left(), player), compare.operator(),
                    renderer.get().fill(compare.right(), player));
            case Condition.Not not -> !test(not.condition(), player);
        };
    }

    static boolean compare(String left, Operator operator, String right) {
        Double a = number(left);
        Double b = number(right);
        if (a == null || b == null) {
            return switch (operator) {
                case EQUAL -> left.equals(right);
                case NOT_EQUAL -> !left.equals(right);
                default -> false;
            };
        }
        int order = Double.compare(a, b);
        return switch (operator) {
            case EQUAL -> order == 0;
            case NOT_EQUAL -> order != 0;
            case AT_LEAST -> order >= 0;
            case AT_MOST -> order <= 0;
            case GREATER -> order > 0;
            case LESS -> order < 0;
        };
    }

    private static Double number(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
