package dev.turtleroles.listener;

import dev.turtleroles.punishment.PunishmentCase;
import dev.turtleroles.service.PunishmentService;
import dev.turtleroles.service.ResourcePackService;
import dev.turtleroles.service.RoleService;
import dev.turtleroles.storage.PlayerRecord;
import dev.turtleroles.storage.PlayerRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class PlayerLifecycleListener implements Listener {
    private final Plugin plugin;
    private final RoleService roles;
    private final PlayerRepository players;
    private final PunishmentService punishments;
    private final ResourcePackService packs;

    public PlayerLifecycleListener(Plugin plugin, RoleService roles, PlayerRepository players, PunishmentService punishments, ResourcePackService packs) {
        this.plugin = plugin;
        this.roles = roles;
        this.players = players;
        this.punishments = punishments;
        this.packs = packs;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        try {
            Optional<PlayerRecord> record = players.findByUuid(event.getUniqueId());
            if (record.isPresent()) {
                Optional<PunishmentCase> ban = punishments.activeBan(record.get());
                if (ban.isPresent()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banMessage(ban.get()));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not determine ban state for " + event.getUniqueId(), e);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("TurtleRoles storage is unavailable. Try again later."));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerRecord record = roles.loadOrCreate(event.getPlayer());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    roles.reconcileOp(event.getPlayer(), record.role());
                    packs.sendOnJoin(event.getPlayer());
                    event.getPlayer().sendMessage(Component.text("Your role is " + record.role().label() + "."));
                });
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not load player role for " + event.getPlayer().getName(), e);
                Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().kick(Component.text("TurtleRoles storage is unavailable. Try again later.")));
            }
        });
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        packs.handleStatus(event);
    }

    private Component banMessage(PunishmentCase ban) {
        String expiry = ban.expiresAt() == null ? "permanent" : "until " + ban.expiresAt();
        return Component.text("You are banned (" + expiry + "). Reason: " + ban.reason());
    }
}
