# KineticMenus

Анимированные меню-сундуки для Paper и Folia 26.2. Меню — это YAML-файлы; открытие, нажатия и фоновое движение — готовые эффекты, которые выбираются по имени, а не рисуются по кадрам. У каждого эффекта свои звуки.

[English version](README.md)

Статус: 0.2.0, предварительная версия перед 1.0.

## Возможности

- Открытие (`open.effect`): `cascade`, `wave`, `spiral`, `typewriter`, `scatter`, `shuffle`, `fall`, `burst`, `none`.
- Переходы между меню: `slide-left`, `slide-right`, `slide-up`, `slide-down`. Старое меню уезжает, новое заезжает следом — два меню работают как страницы.
- Нажатия (`effect` у кнопки): `press`, `flash`, `ripple`, `sparkle`, `shake`, `none`.
- Фон (`ambient.effect`): `chase` — огоньки бегут по рамке, `twinkle` — пустые слоты вспыхивают вразнобой, `pulse` — рамка плавно загорается и гаснет.
- Звуки: у каждого эффекта свои — падающие колокольчики у `cascade`, звонок печатной машинки, щелчки игрового автомата у `shuffle`, шелест страницы у переходов. Заменить: `sound: "ключ громкость высота задержка"`, выключить: `sound: none`.
- Живой текст: `{phase}` в градиенте MiniMessage заставляет цвета переливаться, например `<gradient:#5EE7DF:#B490CA:#5EE7DF:{phase}>Магазин`. Плейсхолдеры PlaceholderAPI обновляются каждые `refresh` тиков.
- Условия: `show-if`, `click-if` и `open-if` с `permission:`, `money:` (Vault) и `placeholder: %player_level% >= 10`; `!` в начале — отрицание. Несколько кнопок могут делить один слот: показывается первая по `priority`, чьё `show-if` выполнено.
- Деньги: `take-money: 100` и `give-money: 100`. Если игроку не хватает, ни одно действие клика не выполняется, а кнопка показывает эффект отказа.
- Свои команды у меню: `commands: [shop, store]` — и `/shop` открывает меню.
- Правка на лету: сохранили файл в `menus/` — он перечитан, открытые меню перерисованы на месте.
- Перенос с DeluxeMenus: `/kmenu import deluxemenus` переводит меню DeluxeMenus в файлы KineticMenus в `menus/imported/`; всё, чему нет замены, перечислено в консоли.
- Удобство: клик во время открытия пропускает анимацию; `/kmenu animations off` и `/kmenu sounds off` — для каждого игрока свои; игроки Bedrock (Geyser с Floodgate) видят меню без анимаций.

Готовые меню: `showcase` (все эффекты), `navigator`, `shop` и `shop-tools` (магазин на две страницы с товаром только для VIP).

## Файл меню

Пример — в английском README, все ключи с комментариями — в `menus/showcase.yml`.

## Действия

`message: <текст>`, `console: <команда>`, `player: <команда>`, `sound: <ключ> [громкость] [высота]`, `open: <меню> [эффект]`, `close`, `refresh`, `take-money: <сумма>`, `give-money: <сумма>`. В командах работают `{player}` и плейсхолдеры PlaceholderAPI. По типу клика: `on-click`, `on-left-click`, `on-right-click`, `on-shift-left-click`, `on-shift-right-click`.

## Команды

| Команда | Право | По умолчанию |
|---|---|---|
| `/kmenu open <меню> [игрок]` | `kineticmenus.command.open`, для других — `.open.others` | все / op |
| `/kmenu animations <on\|off>` | `kineticmenus.command.animations` | все |
| `/kmenu sounds <on\|off>` | `kineticmenus.command.sounds` | все |
| `/kmenu list` | `kineticmenus.command.list` | op |
| `/kmenu reload` | `kineticmenus.command.reload` | op |
| `/kmenu import deluxemenus` | `kineticmenus.command.import` | op |

`/km` — короткий вариант; `kineticmenus.admin` даёт все админские команды.

## Требования

Paper или Folia 26.2, Java 25. Необязательно: PlaceholderAPI, Vault с плагином экономики, Floodgate.

## Ограничения

- Заголовок меню не анимируется: Paper 26.2 больше не поддерживает смену заголовка открытого инвентаря, а переоткрытие на каждом кадре дёргает курсор.
- Команды меню, добавленных во время работы сервера, заработают после перезапуска: сервер регистрирует команды только при старте.
- Импорт DeluxeMenus не переносит задержки, шансы, `[connect]`, требования к предметам и головы с текстурами; о каждом случае есть строка в консоли.

## Сборка

```
./gradlew build
```
