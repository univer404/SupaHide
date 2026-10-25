# SupaHide

A lightweight Paper/Purpur plugin that converts the supplied Skript into a standalone Java plugin.

## Requirements
- Paper or Purpur 1.21.11
- Java 21
- A TAB plugin supporting `/tab player <player> tagprefix`
- A skin plugin supporting the configured `/setskin` command

SupaHide does not bundle TAB or a skin plugin. If your server uses different commands, edit `plugins/SupaHide/config.yml`.

## Command and permission
- `/hide` — toggles the scrambled nametag and Sinsify skin.
- `hide.use` — permission required to toggle the disguise (default: operators).

## Behavior
- First use scrambles the TAB tag prefix with `&k` and applies the `Sinsify` skin.
- Next use restores the tag prefix and sets the player's skin back to their own name.
- The hidden state is stored by UUID in `plugins/SupaHide/data.yml`.
- On join, players who still have `hide.use` have their disguise skin reapplied after 2 ticks. Players who lost the permission are cleared and have their tag and skin restored after 2 ticks.
- Both chat and action-bar feedback are sent.
- Messages support MiniMessage hex colors and legacy ampersand colors.

## Configuration
The generated `config.yml` contains command templates and messages. Use `{player}` as the player-name placeholder. Restart the server after changing command templates.

## Build
Run `mvn clean package` with Java 21. The output is `target/SupaHide.jar`.

GitHub Actions builds the plugin on pushes to `main` and uploads a downloadable `SupaHide.jar` artifact.
