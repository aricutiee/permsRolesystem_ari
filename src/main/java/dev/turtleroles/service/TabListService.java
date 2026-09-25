package dev.turtleroles.service;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

public final class TabListService {
    private static final Key LOGO_FONT = Key.key("turtleroles:header");
    private static final Key DEFAULT_FONT = Key.key("minecraft:default");
    private final Plugin plugin;
    private final PresentationService presentation;
    private BukkitTask task;

    public TabListService(Plugin plugin, PresentationService presentation) {
        this.plugin = plugin;
        this.presentation = presentation;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 1L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void refreshAll() {
        double tps = Math.min(20.0, Bukkit.getTPS()[0]);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            viewer.sendPlayerListHeaderAndFooter(header(viewer), footer(viewer, tps));
        }
    }

    Component header(Player viewer) {
        if (!presentation.canUseGlyphFor(viewer)) {
            return Component.text("SHOCK SMP", NamedTextColor.AQUA).font(DEFAULT_FONT).decorate(TextDecoration.BOLD);
        }
        return Component.empty().font(DEFAULT_FONT)
            // TAB allocates nine pixels per text line, regardless of bitmap
            // height. Five lines reserve 45px before the 47px-ascent glyph,
            // placing its top five pixels inside the header.
            .append(Component.text("\n\n\n\n\n"))
            .append(shock('\uE130')).append(shock('\uE131'))
            .append(Component.text("\n\n").font(DEFAULT_FONT))
            // The wordmark draws downward from this line. Reserve six lines
            // (54px) for its 48px height before the edge decorations/players.
            .append(shock('\uE140'))
            .append(Component.text("\n\n\n\n\n\n"))
            .append(edgePair(false));
    }

    Component footer(Player viewer, double tps) {
        TextColor tpsColor = tps >= 18.0 ? NamedTextColor.GREEN : tps >= 15.0 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        boolean decorated = presentation.canUseGlyphFor(viewer);
        Component result = Component.empty().font(DEFAULT_FONT);
        if (decorated) result = result.append(edgePair(true));
        result = result.append(Component.newline());
        result = result
            .append(Component.text("PING: ", NamedTextColor.GRAY))
            .append(Component.text(viewer.getPing() + "ms", NamedTextColor.AQUA))
            .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
            .append(Component.text("TPS: ", NamedTextColor.GRAY))
            .append(Component.text(String.format(Locale.ROOT, "%.1f", tps), tpsColor));
        if (decorated) result = result.append(Component.newline());
        return result;
    }

    private Component edgePair(boolean footer) {
        return shock('\uE120').append(shock(footer ? '\uE111' : '\uE110'))
            .append(shock(footer ? '\uE122' : '\uE121'));
    }

    private Component shock(char codepoint) {
        return Component.text(String.valueOf(codepoint)).font(LOGO_FONT).color(NamedTextColor.WHITE)
            .decoration(TextDecoration.BOLD, false).decoration(TextDecoration.ITALIC, false);
    }
}
