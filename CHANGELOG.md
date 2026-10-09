# Changelog

## [1.0.0] — 2026-10-09

First release.

### Added

- Menus from YAML files in `menus/` and its subfolders, with MiniMessage texts, animated gradients (`{phase}`) and PlaceholderAPI
- Opening effects: cascade, wave, spiral, typewriter, scatter, shuffle, fall, burst
- Slide transitions between menus: slide-left, slide-right, slide-up, slide-down
- Click effects: press, flash, ripple, sparkle, shake
- Background effects on empty slots: chase, twinkle, pulse
- Sounds made for every effect; `open.sound`, `sound` and `deny-sound` to replace or mute them
- Conditions `open-if`, `show-if`, `click-if`: permission, Vault money, placeholder comparisons, `!` to negate
- Several buttons per slot, chosen by `priority` and `show-if`
- Actions: message, console, player, sound, open, close, refresh, take-money, give-money
- Commands per menu (`commands: [shop]`)
- Menu files apply as soon as they are saved; open menus are redrawn in place
- `/kmenu import deluxemenus`
- Per-player `/kmenu animations off` and `/kmenu sounds off`; Bedrock players get menus without animations
- Bundled menus: `showcase`, `navigator`, `shop`, `shop-tools`
- English and Russian messages
- Paper and Folia 26.2
