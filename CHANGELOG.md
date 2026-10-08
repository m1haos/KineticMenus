# Changelog

## [0.2.0] — 2026-10-08

### Added

- Opening effects `fall` and `burst`, slide transitions between menus
- Click effect `sparkle`; background effects `chase`, `twinkle`, `pulse`
- Sounds for every effect, `sound`, `deny-sound` and `open.sound` to replace or mute them, `/kmenu sounds on|off`, `sounds.volume`
- Conditions `show-if`, `click-if`, `open-if`: permission, Vault money, placeholder comparisons; button priorities for shared slots
- Actions `take-money` and `give-money`
- Commands per menu (`commands: [shop]`)
- Menus reload as soon as a file is saved, open menus are redrawn in place
- `/kmenu import deluxemenus`
- Menus in subfolders of `menus/`
- Bundled menus `navigator`, `shop`, `shop-tools`
- Bedrock players get menus without animations (`bedrock-reduced-motion`)
- Folia support

### Changed

- Smoother animations: easing curves (`open.easing`), sparks ahead of revealed slots, trails behind moving buttons, staggered flights, slot-machine `shuffle`, two-tick holds in `flash` and `shake`
- Default opening length is 16 ticks instead of 12
- `ripple` menu key is now `accent`, with a new `trail` key for faint colours
- `permission` on a button is a shortcut for a `click-if` permission
- `/kmenu reload` keeps open menus open


## [0.1.0] — 2026-10-07

Prototype.

### Added

- Menus from `menus/*.yml` with buttons, filler, MiniMessage texts and PlaceholderAPI
- Opening effects: cascade, wave, spiral, typewriter, scatter, shuffle
- Click effects: press, flash, ripple, shake; permission checks with deny effect and actions
- Animated gradients with `{phase}`
- Actions: message, console, player, sound, open (with an effect for the transition), close, refresh
- `/kmenu open|animations|list|reload`, per-player switch to turn animations off
