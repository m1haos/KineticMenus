# KineticMenus

Animated chest menus for Paper 26.2. Menus are YAML files; opening and click animations are ready-made effects you pick by name instead of drawing frame by frame.

Status: prototype 0.1.0.

## Effects

Opening (`open.effect`): `cascade`, `wave`, `spiral`, `typewriter`, `scatter`, `shuffle`, `none`; length in ticks with `open.ticks`, sound with `open.sound`.

Clicks (`effect` on a button): `press`, `flash`, `ripple`, `shake`, `none`. A button with `permission` the player lacks plays `deny-effect` (default `shake`) and runs `on-deny`.

Live text: `{phase}` in a MiniMessage gradient makes the colours move, e.g. `<gradient:#5EE7DF:#B490CA:#5EE7DF:{phase}>Shop`. PlaceholderAPI placeholders refresh every `refresh` ticks.

A click during the opening skips the rest of it. Players can turn animations off for themselves with `/kmenu animations off`; menus then open instantly.

## Actions

`message: <text>`, `console: <command>`, `player: <command>`, `sound: <key> [volume] [pitch]`, `open: <menu> [effect]`, `close`, `refresh`. `{player}` is replaced with the player name. Per click type: `on-click`, `on-left-click`, `on-right-click`, `on-shift-left-click`, `on-shift-right-click`.

## Commands

| Command | Permission | Default |
|---|---|---|
| `/kmenu open <menu> [player]` | `kineticmenus.command.open`, `.open.others` for others | everyone / op |
| `/kmenu animations <on\|off>` | `kineticmenus.command.animations` | everyone |
| `/kmenu list` | `kineticmenus.command.list` | op |
| `/kmenu reload` | `kineticmenus.command.reload` | op |

`/km` works as a short alias. See `menus/showcase.yml` for every effect in one menu.

## Limits

Menu titles don't animate: Paper 26.2 no longer supports changing the title of an open inventory, and reopening it every frame would make the cursor jump.

## Building

```
./gradlew build
```
