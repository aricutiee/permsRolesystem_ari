# TurtleRoles

TurtleRoles is a Paper 1.21.11 plugin for a strict role hierarchy, moderation cases, SQLite persistence, and automatic delivery of a lightweight role-badge resource pack.

The stored `OWNER` is kept as real Bukkit OP. Any player granted real OP through the server console or an already trusted operator is also treated as owner-level by TurtleRoles, regardless of their stored role, until they are deopped. `CO-OWNER` and `SR ADMIN` use protected TurtleRoles commands unless they have real OP.

## Build

```powershell
.\gradlew.bat clean build
```

Outputs:

- Plugin JAR: `build/libs/TurtleRoles-1.0.0.jar`
- Resource pack: `build/distributions/TurtleRoles-resource-pack.zip`
- SHA-1: `build/distributions/TurtleRoles-resource-pack.sha1`
- Badge PNGs: `build/generated/turtleroles/badges`
- Enlarged previews: `build/generated/turtleroles/previews`

## Install

1. Put `build/libs/TurtleRoles-1.0.0.jar` in the Paper server `plugins` folder.
2. Start once, then stop the server.
3. Host `TurtleRoles-resource-pack.zip` at a public HTTPS URL.
4. Copy that URL and the generated SHA-1 into `plugins/TurtleRoles/config.yml`.
5. Start the server.
6. After the intended Owner has joined once, run from local console:

```text
role bootstrap <player-name-or-known-uuid>
```

Ownership transfer is confirmed:

```text
role transfer <player>
role transfer <player> confirm
```

Players do not manually install the pack. The server sends a normal Minecraft server-pack request on join. In optional mode, declining keeps the player online with colored text fallback. In required mode, declining or timing out disconnects with a clear message.

## Commands

`/role`, `/role info <player>`, `/role set <player> <role> [reason]`, `/role list`, `/role bootstrap`, `/role transfer`.

Moderation: `/warn`, `/warnings`, `/unwarn`, `/tempmute`, `/mute`, `/unmute`, `/tempban`, `/ban`, `/unban`, `/kick`, `/history`, `/case`.

Protected utilities live under `/tr`: `invsee`, `heal`, `feed`, `fly`, `tp`, `tphere`, `clear`, `give`, `time`, `weather`, `reload`, `doctor`, and `pack resend`.

## Known Limits

The plugin protects its own commands, GUI, aliases, and supported chat/private-message command paths. It cannot sandbox malicious plugins, external permission grants, command blocks, or arbitrary third-party commands dispatched as console. Use `/tr doctor` after installing other admin plugins.
