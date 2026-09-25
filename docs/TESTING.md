# TurtleRoles Testing Notes

## Automated Tests

Run:

```powershell
.\gradlew.bat clean build
```

The suite covers:

- strict actor/target rank matrix;
- self-punishment denial and self-service gamemode allowance;
- role grant/demotion restrictions;
- Helper 1 hour and Moderator 7 day duration boundaries, including +1 second denial;
- strict invalid/overflowing duration parsing;
- reversal authority using issuer snapshot rank after demotion;
- SQLite role persistence across restart/name change;
- Owner bootstrap, transfer, and single-owner persistence;
- punishment expiry/revocation persistence;
- failed role mutation rollback;
- generated PNG transparency, height, visible pixels and trimming;
- resource-pack ZIP root structure, font JSON, texture references and unique glyphs;
- resource-pack readiness bound to current connection and revision.

## Manual Server Checklist

These checks require a Paper server and at least one Minecraft Java client:

- Start Paper 1.21.11 with the built JAR in `plugins/`.
- Join once as the intended Owner, then run `role bootstrap <player>` from local console.
- Verify first-time players become MEMBER and returning players keep role after restart and name change.
- Verify Co-owner can assign SR Admin or lower, SR Admin can assign Admin or lower, and Admin cannot assign roles.
- Try self-punishment, equal-rank punishment, and higher-rank punishment from commands and the `/role` GUI.
- Verify Helper tempmute limit is 1 hour and Moderator tempmute/tempban limit is 7 days.
- Verify mutes block chat and configured private-message aliases, including namespaced forms such as `/minecraft:me`.
- Verify tempban remains active across restart and expires from timestamp after its duration.
- Verify `/invsee` is read-only and closes/refreshes safely when the target disconnects.
- Host `TurtleRoles-resource-pack.zip` over public HTTPS, paste URL and SHA-1 into config, and join.
- Confirm the server automatically offers the pack without any player command or manual installation.
- Confirm optional decline keeps colored `[ROLE]` fallback; required decline disconnects with a clear message.
- Confirm successful load upgrades chat badges, and a role change updates glyph display without another pack download.
- Confirm mixed pack acceptance keeps TAB/name-tag fallback until all online viewers have loaded the pack.
- Try namespaced bypass attempts and run `/tr doctor` after installing any other admin/chat/permission plugin.
