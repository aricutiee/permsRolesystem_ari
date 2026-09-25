package dev.turtleroles.service;

import dev.turtleroles.pack.PackStatus;
import dev.turtleroles.role.Role;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PresentationService {
    private static final Key ROLE_FONT = Key.key("turtleroles:roles");
    private static final Key DEFAULT_FONT = Key.key("minecraft:default");
    private final RoleService roleService;
    private final Map<UUID, PackStatus> packStatuses = new ConcurrentHashMap<>();

    public PresentationService(RoleService roleService) {
        this.roleService = roleService;
    }

    public void setPackStatus(UUID player, PackStatus status) {
        packStatuses.put(player, status);
    }

    public boolean canUseGlyphFor(Player viewer) {
        return packStatuses.get(viewer.getUniqueId()) == PackStatus.LOADED;
    }

    public Component badgeFor(Role role, boolean glyph) {
        if (glyph) {
            // Keep the bitmap font on the glyph child only. A custom font on
            // the root leaks into spaces, chat separators and message text,
            // which renders every unsupported character as a missing-glyph box.
            return Component.empty().font(DEFAULT_FONT)
                .append(Component.text(role.glyph())
                    .font(ROLE_FONT)
                    .color(NamedTextColor.WHITE)
                    .decoration(TextDecoration.BOLD, false)
                    .decoration(TextDecoration.ITALIC, false));
        }
        return fallback(role);
    }

    public Component displayNameFor(Player subject, boolean glyph) {
        Role role = roleService.roleOf(subject.getUniqueId());
        return Component.empty().font(DEFAULT_FONT)
            .append(badgeFor(role, glyph))
            .append(Component.space().font(DEFAULT_FONT))
            .append(Component.text(subject.getName(), NamedTextColor.WHITE).font(DEFAULT_FONT));
    }

    public Component chatNameFor(Player subject, Player viewer) {
        Role role = roleService.roleOf(subject.getUniqueId());
        Component playerName = Component.text(subject.getName(), NamedTextColor.WHITE)
            .font(DEFAULT_FONT);
        if (role == Role.MEMBER) {
            return playerName;
        }
        return Component.empty().font(DEFAULT_FONT)
            .append(badgeFor(role, canUseGlyphFor(viewer)))
            .append(Component.space().font(DEFAULT_FONT))
            .append(playerName);
    }

    public void refreshPlayer(Player player) {
        boolean universalGlyph = !Bukkit.getOnlinePlayers().isEmpty() && Bukkit.getOnlinePlayers().stream().allMatch(this::canUseGlyphFor);
        Component name = displayNameFor(player, universalGlyph);
        player.displayName(name);
        player.playerListName(name);
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshPlayer(player);
        }
    }

    private Component fallback(Role role) {
        if (role == Role.OWNER) {
            return Component.text("[", TextColor.color(0x5BCEFA))
                .append(Component.text("OWNER", TextColor.color(0xF5A9B8)))
                .append(Component.text("]", TextColor.color(0x5BCEFA)));
        }
        TextColor color = switch (role) {
            case CO_OWNER -> TextColor.color(0x3E9161);
            case SR_ADMIN -> TextColor.color(0xC8CED8);
            case ADMIN -> TextColor.color(0xBD2142);
            case MODERATOR -> TextColor.color(0x41CE70);
            case HELPER -> TextColor.color(0xFFE477);
            case MEMBER -> TextColor.color(0xAEB5C0);
            case OWNER -> TextColor.color(0xFFFFFF);
        };
        return Component.text("[" + role.label() + "]", color);
    }
}
