# KineticMenus

Animated chest menus for Paper and Folia 26.2. Menus are YAML files; openings, clicks and background motion are ready-made effects you pick by name instead of drawing frame by frame, and each effect comes with sounds made for it.

[Русская версия](README.ru.md)

Status: 0.2.0, preview before 1.0.

## What's in the box

- Openings (`open.effect`): `cascade`, `wave`, `spiral`, `typewriter`, `scatter`, `shuffle`, `fall`, `burst`, `none`.
- Transitions between menus: `slide-left`, `slide-right`, `slide-up`, `slide-down`. The old menu moves out while the new one moves in, so two menus work like pages.
- Clicks (`effect` on a button): `press`, `flash`, `ripple`, `sparkle`, `shake`, `none`.
- Background (`ambient.effect`): `chase` runs lights around the border, `twinkle` lights empty slots at random, `pulse` makes the border glow and fade.
- Sounds: every effect has its own: falling chimes for `cascade`, a typewriter bell, slot machine clicks for `shuffle`, a page turn for slides. Replace them with `sound: "key volume pitch delay"` or turn them off with `sound: none`.
- Live text: `{phase}` in a MiniMessage gradient makes the colours move, for example `<gradient:#5EE7DF:#B490CA:#5EE7DF:{phase}>Shop`. PlaceholderAPI placeholders refresh every `refresh` ticks.
- Conditions: `show-if`, `click-if` and `open-if` with `permission:`, `money:` (Vault) and `placeholder: %player_level% >= 10`; `!` in front negates. Several buttons can share a slot, the first one by `priority` whose `show-if` is met is shown.
- Money: `take-money: 100` and `give-money: 100`. If the player cannot pay, none of the click's actions run and the button plays its deny effect.
- Commands per menu: `commands: [shop, store]` makes `/shop` open the menu.
- Live editing: saving a file in `menus/` reloads it and redraws open menus in place.
- DeluxeMenus import: `/kmenu import deluxemenus` turns DeluxeMenus menus into KineticMenus files in `menus/imported/`; anything without a counterpart is listed in the console.
- Comfort: a click during an opening skips it; `/kmenu animations off` and `/kmenu sounds off` work per player; Bedrock players (Geyser with Floodgate) get menus without animations.

Bundled menus: `showcase` (every effect), `navigator`, `shop` and `shop-tools` (a two-page shop with VIP-only goods).

## Menu file

```yaml
title: "<gradient:#F2C14E:#FF6B6B>Shop"
rows: 5
commands: [shop]
open:
  effect: fall
  ticks: 16
ambient:
  effect: pulse
filler:
  material: black_stained_glass_pane
  hide-tooltip: true
buttons:
  bread:
    slot: 20
    material: bread
    name: "<#F2C14E>Bread ×16"
    click-if: ["money: 20"]
    on-click:
      - "take-money: 20"
      - "console: give {player} minecraft:bread 16"
    effect: sparkle
    on-deny: ["message: <#FF6B6B>You need 20 for that."]
  tools:
    slot: 26
    material: iron_pickaxe
    on-click: ["open: shop-tools slide-left"]
```

`menus/showcase.yml` lists every key with a comment.

## Actions

`message: <text>`, `console: <command>`, `player: <command>`, `sound: <key> [volume] [pitch]`, `open: <menu> [effect]`, `close`, `refresh`, `take-money: <amount>`, `give-money: <amount>`. `{player}` and PlaceholderAPI placeholders work in commands. Per click type: `on-click`, `on-left-click`, `on-right-click`, `on-shift-left-click`, `on-shift-right-click`.

## Commands

| Command | Permission | Default |
|---|---|---|
| `/kmenu open <menu> [player]` | `kineticmenus.command.open`, `.open.others` for others | everyone / op |
| `/kmenu animations <on\|off>` | `kineticmenus.command.animations` | everyone |
| `/kmenu sounds <on\|off>` | `kineticmenus.command.sounds` | everyone |
| `/kmenu list` | `kineticmenus.command.list` | op |
| `/kmenu reload` | `kineticmenus.command.reload` | op |
| `/kmenu import deluxemenus` | `kineticmenus.command.import` | op |

`/km` is a short alias; `kineticmenus.admin` gives every admin command.

## Requirements

Paper or Folia 26.2, Java 25. Optional: PlaceholderAPI, Vault with an economy plugin, Floodgate.

## Limits

- Menu titles don't animate: Paper 26.2 no longer supports changing the title of an open inventory, and reopening it every frame would make the cursor jump.
- Commands of menus added while the server runs start working after a restart; the server only registers commands on start.
- The DeluxeMenus import leaves out delays, chances, `[connect]`, item requirements and textured heads; each one is reported.

## Building

```
./gradlew build
```
