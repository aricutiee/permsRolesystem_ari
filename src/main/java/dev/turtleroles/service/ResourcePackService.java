package dev.turtleroles.service;

import dev.turtleroles.config.TurtleConfig;
import dev.turtleroles.pack.PackStatus;
import dev.turtleroles.pack.PlayerPackState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;

import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ResourcePackService {
    private final Plugin plugin;
    private final PresentationService presentation;
    private final Map<UUID, PlayerPackState> states = new ConcurrentHashMap<>();
    private TurtleConfig.ResourcePackSettings settings;
    private String revision = "unconfigured";

    public ResourcePackService(Plugin plugin, PresentationService presentation, TurtleConfig.ResourcePackSettings settings) {
        this.plugin = plugin;
        this.presentation = presentation;
        updateSettings(settings);
    }

    public void updateSettings(TurtleConfig.ResourcePackSettings settings) {
        this.settings = settings;
        this.revision = settings.sha1().isBlank() ? "missing-sha1" : settings.sha1().toLowerCase();
    }

    public void sendOnJoin(Player player) {
        PackStatus initialStatus = settings.autoSendOnJoin()
            ? PackStatus.NOT_SENT
            : mapStatus(player.getResourcePackStatus());
        states.put(player.getUniqueId(), new PlayerPackState(UUID.randomUUID(), revision, initialStatus, 0));
        presentation.setPackStatus(player.getUniqueId(), initialStatus);
        if (settings.enabled() && settings.autoSendOnJoin()) {
            send(player);
        }
    }

    public void send(Player player) {
        if (!settings.enabled() || settings.url().isBlank() || settings.sha1().isBlank()) {
            plugin.getLogger().warning("TurtleRoles resource pack is enabled but URL or SHA-1 is missing. Text fallback remains active.");
            return;
        }
        PlayerPackState old = states.get(player.getUniqueId());
        int attempts = old == null ? 0 : old.attempts();
        if (attempts > settings.maxRetries()) {
            return;
        }
        byte[] hash;
        try {
            hash = HexFormat.of().parseHex(settings.sha1().replace(" ", ""));
            if (hash.length != 20) {
                throw new IllegalArgumentException("SHA-1 must be 20 bytes.");
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Invalid TurtleRoles resource-pack SHA-1; text fallback remains active.", e);
            return;
        }
        UUID connection = old == null ? UUID.randomUUID() : old.connectionId();
        states.put(player.getUniqueId(), new PlayerPackState(connection, revision, PackStatus.PENDING, attempts + 1));
        presentation.setPackStatus(player.getUniqueId(), PackStatus.PENDING);
        // Modern clients identify and cache server packs by UUID. Supplying the
        // configured ID is especially important when a proxy transfers a player
        // to this Paper backend, as Minekeep does.
        player.setResourcePack(settings.uuid(), settings.url(), hash, settings.prompt(), settings.required());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerPackState state = states.get(player.getUniqueId());
            if (state != null && state.connectionId().equals(connection) && state.revision().equals(revision) && state.status() == PackStatus.PENDING) {
                states.put(player.getUniqueId(), new PlayerPackState(connection, revision, PackStatus.TIMED_OUT, state.attempts()));
                presentation.setPackStatus(player.getUniqueId(), PackStatus.TIMED_OUT);
                presentation.refreshAll();
                if (settings.required() && player.isOnline()) {
                    player.kick(net.kyori.adventure.text.Component.text("TurtleRoles badges are required on this server. Please accept the server resource pack to play."));
                }
            }
        }, Math.max(20L, settings.timeoutSeconds() * 20L));
    }

    public void handleStatus(PlayerResourcePackStatusEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        plugin.getLogger().info("Resource pack status for " + event.getPlayer().getName()
            + ": " + event.getStatus() + " (pack " + event.getID() + ")");
        PlayerPackState old = states.get(playerId);
        if (old == null || !old.revision().equals(revision) || !settings.uuid().equals(event.getID())) {
            return;
        }
        PackStatus status = mapStatus(event.getStatus());
        states.put(playerId, new PlayerPackState(old.connectionId(), old.revision(), status, old.attempts()));
        presentation.setPackStatus(playerId, status);
        presentation.refreshAll();
        if (settings.required()
            && (status == PackStatus.DECLINED || status == PackStatus.FAILED)
            && event.getPlayer().isOnline()) {
            event.getPlayer().kick(Component.text(
                "TurtleRoles badges are required on this server. Rejoin and accept the server resource pack to play."
            ));
        }
    }

    private PackStatus mapStatus(PlayerResourcePackStatusEvent.Status status) {
        return switch (status) {
            case SUCCESSFULLY_LOADED -> PackStatus.LOADED;
            case DECLINED -> PackStatus.DECLINED;
            case FAILED_DOWNLOAD, FAILED_RELOAD, INVALID_URL, DISCARDED -> PackStatus.FAILED;
            default -> PackStatus.PENDING;
        };
    }

    public PlayerPackState state(Player player) {
        return states.get(player.getUniqueId());
    }
}
