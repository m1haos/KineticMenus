# KineticMenus guide

[Русская версия](guide.ru.md)

1. [Installation](#installation)
2. [How a menu file is built](#how-a-menu-file-is-built)
3. [Menu keys](#menu-keys)
4. [Buttons](#buttons)
5. [Icons and text](#icons-and-text)
6. [Effects](#effects)
7. [Sounds](#sounds)
8. [Conditions](#conditions)
9. [Actions](#actions)
10. [Money](#money)
11. [Pages and transitions](#pages-and-transitions)
12. [Commands and permissions](#commands-and-permissions)
13. [config.yml](#configyml)
14. [Moving from DeluxeMenus](#moving-from-deluxemenus)
15. [Questions](#questions)

## Installation

1. Put `KineticMenus.jar` into `plugins/` and start the server.
2. Open `/showcase` in game: every effect is there, one button each.
3. Look at `plugins/KineticMenus/menus/`. The bundled menus `navigator.yml`, `shop.yml` and `shop-tools.yml` are good starting points for your own.

Needs Paper or Folia 26.2 and Java 25. PlaceholderAPI, Vault (or VaultUnlocked) with an economy plugin and Floodgate are optional.

Menu files are watched: save one and the change is in game a moment later, menus that players have open included. `/kmenu reload` does the same by hand and also rereads `config.yml` and the language files.

## How a menu file is built

Every `.yml` file in `menus/` is one menu, subfolders included. The file name is the menu id: `menus/shop/tools.yml` is the menu `tools`. Two files with the same name in different folders clash; one of them is skipped with a warning.

```yaml
title: "<gradient:#F2C14E:#FF6B6B>Shop"
rows: 5
commands: [shop]

open:
  effect: fall

ambient:
  effect: pulse

filler:
  material: black_stained_glass_pane
  hide-tooltip: true

buttons:
  bread:
    slot: 20
    material: bread
    name: "<#F2C14E>Bread"
    on-click: ["console: give {player} minecraft:bread 16"]
    effect: sparkle
```

A mistake never stops the menu from loading. The console names the file and the key and says what was expected:

```
menus/shop.yml, buttons.bread.effect: 'sparkel' is not one of [none, press, flash, shake, ripple, sparkle].
```

## Menu keys

| Key | Default | What it does |
|---|---|---|
| `title` | the menu id | Title in MiniMessage. `{player}` and PlaceholderAPI work, `{phase}` does not: Paper cannot change the title of an open menu. |
| `rows` | `3` | Height, 1 to 6. |
| `command` / `commands` | none | Commands that open the menu, without the slash: `commands: [shop, store]`. |
| `permission` | none | Permission needed to open the menu. Short for a `permission:` line in `open-if`. |
| `open-if` | none | [Conditions](#conditions) checked before the menu opens. |
| `on-deny` | the `no-access` message | [Actions](#actions) run instead of opening when `open-if` is not met. |
| `open.effect` | `none` | [Opening effect](#opening). |
| `open.ticks` | `16` | Length of the opening, 1 to 100 ticks (20 ticks are a second). |
| `open.easing` | `auto` | Speed curve: `auto` (even speed), `ease-in`, `ease-out`, `ease-in-out`. |
| `open.sound` | `auto` | [Sounds](#sounds) of the opening. |
| `ambient.effect` | `none` | [Background effect](#background). |
| `ambient.speed` | `2` | Ticks per step of the background effect, 1 to 40. |
| `refresh` | `2` | Ticks between redraws of gradients, placeholders and `show-if`, 1 to 200. Only menus that have any of those are redrawn. |
| `filler` | none | [Icon](#icons-and-text) for slots without a button. Without it they stay empty. |
| `accent` | light blue pane | Bright colour of effects: ripple fronts, sparks, border lights, the leading edge of an opening. |
| `trail` | gray pane | Faint colour effects leave behind. |
| `buttons` | none | [Buttons](#buttons), one section each. |

## Buttons

```yaml
buttons:
  vip-crate:
    slot: 24
    priority: 1
    material: ender_chest
    name: "<gradient:#C38BFF:#7FB3FF>VIP crate"
    show-if: ["permission: shop.vip"]
    click-if: ["money: 250"]
    on-click:
      - "take-money: 250"
      - "console: give {player} minecraft:diamond 2"
    effect: sparkle
    on-deny: ["message: <#FF6B6B>You need 250 for that."]
```

| Key | Default | What it does |
|---|---|---|
| `slot` / `slots` | none | Slot numbers from 0, left to right, top to bottom. Ranges work: `slots: ["0-8", 36, "45-53"]`. A button without a slot inside the menu is skipped. |
| `priority` | `0` | When several buttons share a slot, the one with the lowest number whose `show-if` is met is shown. |
| icon keys | | `material`, `amount`, `name`, `lore`, `glint`, `model`, `hide-tooltip`, see [Icons and text](#icons-and-text). |
| `pressed` | the icon with glint | Icon shown while the `press` effect holds the button down. |
| `show-if` | none | [Conditions](#conditions) for the button to be shown at all. |
| `click-if` | none | Conditions for a click to go through. |
| `permission` | none | Short for a `permission:` line in `click-if`. |
| `effect` | `press` | [Click effect](#clicks). |
| `sound` | `auto` | [Sounds](#sounds) of the click. |
| `on-click` | none | [Actions](#actions) for any click. |
| `on-left-click`, `on-right-click`, `on-shift-left-click`, `on-shift-right-click` | `on-click` | Actions for one kind of click. A kind without its own list falls back to `on-click`. |
| `deny-effect` | `shake` | Effect when `click-if` is not met or the player cannot pay. |
| `deny-sound` | `auto` | Sounds of a refusal. |
| `on-deny` | none | Actions of a refusal. Without them a refused purchase says `not-enough-money`; other refusals only shake. |

Clicks closer than 3 ticks to the previous one are ignored, so a double click never buys twice. A click while the menu is still opening skips the opening.

Menu items are never real: no click, shift-click, number key, drag or drop takes anything out of a menu or puts anything in.

## Icons and text

| Key | Default | What it does |
|---|---|---|
| `material` | `stone` | Any item. An unknown name is replaced by a barrier, with a warning. |
| `amount` | `1` | Stack size shown on the item. |
| `name` | the item's own | Name in MiniMessage. |
| `lore` | none | Lines in MiniMessage. |
| `glint` | `false` | Enchantment glint without enchantments. |
| `model` | none | Item model from a resource pack, `namespace:path`. |
| `hide-tooltip` | `false` | No tooltip at all, handy for fillers. |

Names and lore are not italic unless you ask for `<italic>`. Text can hold:

- `{player}`, the player's name;
- PlaceholderAPI placeholders, when PlaceholderAPI is installed;
- `{phase}`, a number that runs from -1 to 1 every two seconds. Put it last in a gradient and the colours move: `<gradient:#5EE7DF:#B490CA:#5EE7DF:{phase}>Shop`. Start and end the gradient with the same colour and the loop has no visible jump.

## Effects

### Opening

| Effect | What players see |
|---|---|
| `none` | The menu at once. |
| `cascade` | Row by row from the top, each row sweeping in from the left. |
| `wave` | Rings from the middle outwards. |
| `spiral` | Along the border and inwards. |
| `typewriter` | Slot after slot, with a bell at the end. |
| `scatter` | Buttons fly in from random slots. |
| `shuffle` | Buttons spin like slot machine reels and stop column by column. |
| `fall` | Buttons drop in from above, left columns first. |
| `burst` | Buttons fly out of the middle. |
| `slide-left`, `slide-right`, `slide-up`, `slide-down` | The previous menu moves out and this one follows. See [Pages and transitions](#pages-and-transitions). |

`cascade`, `wave`, `spiral` and `typewriter` reveal the filler together with the buttons, with a spark of the `accent` colour just ahead. `scatter`, `fall`, `burst` and `shuffle` move only the buttons over a filler that is there from the start, and leave a `trail` behind.

`open.ticks` is the length of the opening. Buttons that only travel a short way arrive sooner rather than crawl, so `scatter`, `fall` and `burst` may finish early. Slides always move one column (or row) per tick.

### Clicks

| Effect | What players see |
|---|---|
| `none` | Nothing. |
| `press` | The button sinks in for a moment (shows `pressed`). |
| `flash` | Two blinks of glint. |
| `ripple` | A ring of `accent` spreads over the empty slots around the button, followed by `trail`. |
| `sparkle` | The button glows while sparks fly around it. For purchases and rewards. |
| `shake` | The button jerks left and right. The default refusal. |

Click effects only draw on the button and on empty slots, never over other buttons.

### Background

| Effect | What players see |
|---|---|
| `none` | A still background. |
| `chase` | Lights run around the border, two of them on large menus. |
| `twinkle` | Empty slots light up one by one at random. |
| `pulse` | The border slowly glows and fades. |

Background effects only use empty slots and stop while a menu is opening.

Players who turned animations off (`/kmenu animations off`) and Bedrock players see none of the effects; menus open at once and buttons stay still. Sounds stay.

## Sounds

Every effect comes with its own sounds: falling chimes for `cascade`, a typewriter bell, slot machine clicks for `shuffle`, a page turn for slides, a click and release for `press`, a low double "no" for `shake`. `open.sound`, `sound` and `deny-sound` take:

- `auto`, the effect's own sounds (default);
- `none`, silence;
- your own sounds, one per line: `"key volume pitch delay"`. Volume and pitch default to 1, delay to 0 ticks.

```yaml
sound:
  - "block.note_block.chime 0.6 1.2"
  - "block.note_block.chime 0.6 1.6 3"
```

Keys are the vanilla sound names, with or without `minecraft:`. Menu sounds play in the UI sound category. `sounds.volume` in `config.yml` scales all of them, and `/kmenu sounds off` silences them for one player.

## Conditions

`open-if`, `show-if` and `click-if` take a list; every line must be met.

| Line | Met when |
|---|---|
| `permission: shop.vip` | The player has the permission. |
| `money: 100` | The player has at least 100. Needs Vault and an economy plugin; without them it is never met. |
| `placeholder: %player_level% >= 10` | The comparison holds. Both sides may hold placeholders. |

Comparisons: `==`, `!=`, `>=`, `<=`, `>`, `<`. Numbers are compared as numbers; anything else as text, where only `==` and `!=` work.

`!` in front turns a line around: `"!permission: shop.vip"` is met for players without the permission.

`show-if` is checked again on every redraw and on the tick after each click, so a button appears as soon as the player gets the permission or the money.

## Actions

Actions run from top to bottom.

| Line | What it does |
|---|---|
| `message: <text>` | Sends a MiniMessage line to the player. |
| `console: <command>` | Runs a command as the console. |
| `player: <command>` | Runs a command as the player. |
| `sound: <key> [volume] [pitch]` | Plays a sound to the player. |
| `open: <menu> [effect]` | Opens another menu, with its own opening or the effect given here. |
| `close` | Closes the menu. |
| `refresh` | Redraws the menu at once. |
| `take-money: <amount>` | Takes money, see [Money](#money). |
| `give-money: <amount>` | Gives money. |

`{player}` and PlaceholderAPI placeholders work in commands; a leading slash is fine. `open` and `close` happen on the next tick: a menu cannot be swapped from inside its own click.

## Money

```yaml
click-if: ["money: 20"]
on-click:
  - "take-money: 20"
  - "console: give {player} minecraft:bread 16"
on-deny: ["message: <#FF6B6B>You need 20 for that."]
```

Before any action of a click runs, KineticMenus adds up every `take-money` in it and checks the balance. If the player cannot pay, nothing runs at all and the button plays its refusal. Keep `take-money` first in the list: if the economy refuses the payment at the last moment, the actions after it don't run.

Works with Vault or VaultUnlocked and any economy plugin that registers with them (EssentialsX and others). The economy is found when it is needed, so the order in which plugins start does not matter.

## Pages and transitions

A slide shows the menu the player had open moving out while the new one moves in:

```yaml
# shop.yml
tools:
  slot: 26
  material: iron_pickaxe
  on-click: ["open: shop-tools slide-left"]

# shop-tools.yml
back:
  slot: 36
  material: arrow
  on-click: ["open: shop slide-right"]
```

Any effect can be given to `open:`; slides are the ones that use the previous menu. Opened by a command, a slide comes in over an empty screen.

## Commands and permissions

| Command | Permission | Default |
|---|---|---|
| `/kmenu open <menu> [player]` | `kineticmenus.command.open`; `kineticmenus.command.open.others` for other players | everyone / op |
| `/kmenu animations <on\|off>` | `kineticmenus.command.animations` | everyone |
| `/kmenu sounds <on\|off>` | `kineticmenus.command.sounds` | everyone |
| `/kmenu list` | `kineticmenus.command.list` | op |
| `/kmenu reload` | `kineticmenus.command.reload` | op |
| `/kmenu import deluxemenus` | `kineticmenus.command.import` | op |

`/km` is a short form of `/kmenu`. `kineticmenus.admin` gives every admin command. The animation and sound switches are stored on the player and survive restarts.

Menu commands (`commands:` in a menu) need no permission of their own; use `permission` or `open-if` in the menu. Commands of menus added while the server is running work after a restart: the server registers commands only when it starts. `/kmenu reload` tells you when that is the case.

## config.yml

```yaml
config-version: 2
language: en
auto-reload: true
sounds:
  enabled: true
  volume: 1.0
bedrock-reduced-motion: true
```

| Key | What it does |
|---|---|
| `language` | `en` or `ru`. Texts are in `lang/` and can be edited; missing lines are taken from the plugin. |
| `auto-reload` | Apply saved menu files at once. |
| `sounds.enabled` | Turns every menu sound off for everyone. |
| `sounds.volume` | Multiplier for menu sounds, 0 to 2. |
| `bedrock-reduced-motion` | Menus without animations for Bedrock players (Geyser with Floodgate): their client redraws chest menus slowly and fast animations turn into flicker. |

New keys are added to your `config.yml` on update, your values stay.

## Moving from DeluxeMenus

1. Keep the `plugins/DeluxeMenus` folder where it is (DeluxeMenus itself may be removed).
2. Run `/kmenu import deluxemenus`.
3. The menus appear in `plugins/KineticMenus/menus/imported/` and load at once. The console lists everything that needs a look.

Menus that already have a file are left alone, so the import can be repeated safely.

| DeluxeMenus | KineticMenus |
|---|---|
| `menu_title`, `size`, `open_command`, `update_interval` | `title`, `rows`, `commands`, `refresh` |
| `open_requirement` | `open-if`, `on-deny` |
| `material`, `amount`, `slot`, `slots`, `priority` | same keys |
| `display_name`, `lore` (`&` colours and `&#RRGGBB`) | `name`, `lore` in MiniMessage |
| `enchantments`, `enchantment_glint_override` | `glint: true` |
| `item_model`, `hide_tooltip` | `model`, `hide-tooltip` |
| `view_requirement` | `show-if` |
| `click_requirement` (or the first per-click one) | `click-if`, `on-deny` |
| `click_commands`, `left_click_commands` and the other per-click lists | `on-click`, `on-left-click` and so on |
| `has permission`, `has permissions`, `has money`, `string equals`, `==`, `>=` and the other comparisons, with `!` | `permission:`, `money:`, `placeholder:` |
| `[player]`, `[console]`, `[commandevent]`, `[message]`, `[minimessage]`, `[openguimenu]`, `[close]`, `[refresh]`, `[sound]`, `[takemoney]`, `[givemoney]` | the matching actions |

Left out, each with a console line: textured and plugin heads (`head-`, `basehead-`, `hdb-` and the like become a player head), `model_data`, `minimum_requirements`, `middle_click_commands`, delays and chances on actions, `[connect]`, `[broadcast]`, `[json]` and other actions without a counterpart, item and experience requirements, JavaScript requirements, buttons made of air. Imported menus open with `cascade`; change `open.effect` to taste.

## Questions

**Why doesn't the title animate?** Paper 26.2 does not support changing the title of an open menu. Plugins that animate titles reopen the menu every frame, which makes the cursor jump to the middle of the screen.

**Does it work on Folia?** Yes. Every menu runs on its viewer's own thread.

**Does it slow the server down?** A menu does its work only while it is open, and only slots whose item actually changed are sent to the player. A menu without gradients, placeholders, `show-if` or a background effect sends nothing at all once the opening is over.

**A player says the menu flickers.** They may be on Bedrock without Floodgate, or simply prefer it calm: `/kmenu animations off` is theirs to use.

**Where are player settings stored?** On the player (persistent data), nothing on disk in the plugin folder.
