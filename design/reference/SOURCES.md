# Official technical references

Verified while preparing the handoff on 2026-09-19. The project intentionally targets 1.21.11; newer Minecraft/Paper versions use different dependencies and may need another Java version or resource-pack format.

- [Paper setup and Java compatibility](https://docs.papermc.io/paper/getting-started/) — the compatibility table specifies Java 21 for Paper 1.20 through 1.21.11.
- [Paper project setup](https://docs.papermc.io/paper/dev/project-setup/) — official Maven repository and the pre-26.1 dependency version convention.
- [Paper 1.21.11 API](https://jd.papermc.io/paper/1.21.11/) — target-version API reference.
- [ServerOperator](https://jd.papermc.io/paper/1.21.11/org/bukkit/permissions/ServerOperator.html) — operator status is a boolean grant. The need for protected target-aware commands is an engineering conclusion from this model, not a built-in Bukkit hierarchical OP feature.
- [Paper chat events](https://docs.papermc.io/paper/dev/chat-events/) — Adventure chat rendering and asynchronous event handling.
- [Adventure custom-font formatting](https://docs.papermc.io/adventure/minimessage/format/#font) — custom font keys reference fonts supplied by resource packs.
- [Adventure resource packs](https://docs.papermc.io/adventure/resource-pack/) — client pack delivery, identity/hash and acceptance behavior.
- [Minecraft Java Edition 1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11) — resource pack version 75.0.
- [Minecraft Java Edition 1.21.9](https://www.minecraft.net/de-de/article/minecraft-java-edition-1-21-9) — modern `min_format`/`max_format` metadata format.
