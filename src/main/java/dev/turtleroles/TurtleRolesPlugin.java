package dev.turtleroles;

import dev.turtleroles.command.GameModeCommand;
import dev.turtleroles.command.ModerationCommand;
import dev.turtleroles.command.RoleCommand;
import dev.turtleroles.command.TurtleCommand;
import dev.turtleroles.config.TurtleConfig;
import dev.turtleroles.gui.RoleMenuService;
import dev.turtleroles.listener.ChatAndCommandListener;
import dev.turtleroles.listener.PlayerLifecycleListener;
import dev.turtleroles.policy.PolicyService;
import dev.turtleroles.service.InventoryInspectService;
import dev.turtleroles.service.PresentationService;
import dev.turtleroles.service.PunishmentService;
import dev.turtleroles.service.ResourcePackService;
import dev.turtleroles.service.ResourcePackHttpServer;
import dev.turtleroles.service.RoleService;
import dev.turtleroles.service.TabListService;
import dev.turtleroles.storage.PlayerRepository;
import dev.turtleroles.storage.PunishmentRepository;
import dev.turtleroles.storage.SQLiteDatabase;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;

public final class TurtleRolesPlugin extends JavaPlugin {
    private SQLiteDatabase database;
    private InventoryInspectService invsee;
    private ResourcePackHttpServer packHttpServer;
    private TabListService tabList;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("roles.yml", false);

        TurtleConfig config = TurtleConfig.from(getConfig());
        try {
            database = new SQLiteDatabase(Path.of(getDataFolder().getAbsolutePath(), "turtleroles.db"));
            database.open();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "TurtleRoles storage failed to open; disabling plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlayerRepository players = new PlayerRepository(database);
        PunishmentRepository punishmentRepository = new PunishmentRepository(database);
        PolicyService policy = new PolicyService(config.helperMuteLimit(), config.moderatorMuteLimit(), config.moderatorBanLimit());
        RoleService roles = new RoleService(this, players);
        PunishmentService punishments = new PunishmentService(policy, players, punishmentRepository);
        PresentationService presentation = new PresentationService(roles);
        tabList = new TabListService(this, presentation);
        if (config.resourcePack().selfHostEnabled()) {
            try {
                packHttpServer = new ResourcePackHttpServer(this, config.resourcePack().selfHostPort());
                packHttpServer.start();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Could not start the TurtleRoles resource-pack web server.", e);
            }
        }
        ResourcePackService packs = new ResourcePackService(this, presentation, config.resourcePack());
        RoleMenuService menus = new RoleMenuService(this, roles, players, policy, presentation);
        invsee = new InventoryInspectService(this, roles, policy, config.invseeRefreshTicks());
        invsee.start();
        tabList.start();

        RoleCommand roleCommand = new RoleCommand(roles, players, policy, menus, presentation, config);
        setExecutor("role", roleCommand);
        setTabCompleter("role", roleCommand);
        setExecutor("warn", new ModerationCommand("warn", roles, players, punishments, config));
        setExecutor("tempmute", new ModerationCommand("tempmute", roles, players, punishments, config));
        setExecutor("mute", new ModerationCommand("mute", roles, players, punishments, config));
        setExecutor("tempban", new ModerationCommand("tempban", roles, players, punishments, config));
        setExecutor("ban", new ModerationCommand("ban", roles, players, punishments, config));
        setExecutor("kick", new ModerationCommand("kick", roles, players, punishments, config));
        setExecutor("unwarn", new ModerationCommand("unwarn", roles, players, punishments, config));
        setExecutor("unmute", new ModerationCommand("unmute", roles, players, punishments, config));
        setExecutor("unban", new ModerationCommand("unban", roles, players, punishments, config));
        setExecutor("warnings", new ModerationCommand("warnings", roles, players, punishments, config));
        setExecutor("history", new ModerationCommand("history", roles, players, punishments, config));
        setExecutor("case", new ModerationCommand("case", roles, players, punishments, config));
        setExecutor("gamemode", new GameModeCommand(roles, players, policy));
        TurtleCommand utilities = new TurtleCommand(this, roles, players, policy, invsee, presentation, packs, config);
        setExecutor("tr", utilities);
        setExecutor("invsee", (sender, command, label, args) -> {
            String[] forwarded = new String[args.length + 1];
            forwarded[0] = "invsee";
            System.arraycopy(args, 0, forwarded, 1, args.length);
            return utilities.onCommand(sender, command, label, forwarded);
        });

        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(invsee, this);
        getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, roles, players, punishments, packs), this);
        getServer().getPluginManager().registerEvents(new ChatAndCommandListener(this, players, punishments, presentation, config), this);

        roles.reconcileOnlineOps();
        if (config.resourcePack().url().isBlank() || config.resourcePack().sha1().isBlank()) {
            getLogger().warning("Resource-pack URL/SHA-1 is not configured. Automatic delivery is enabled, but players will see colored text fallback until the generated ZIP is hosted and config.yml is updated.");
        }
    }

    @Override
    public void onDisable() {
        if (packHttpServer != null) {
            packHttpServer.stop();
        }
        if (invsee != null) {
            invsee.stop();
        }
        if (tabList != null) {
            tabList.stop();
        }
        if (database != null) {
            try {
                database.close();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed closing TurtleRoles database.", e);
            }
        }
    }

    private void setExecutor(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command " + name + " in plugin.yml");
        command.setExecutor(executor);
    }

    private void setTabCompleter(String name, org.bukkit.command.TabCompleter completer) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command " + name + " in plugin.yml");
        command.setTabCompleter(completer);
    }
}
