package net.thecommandcraft.vanishpp.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;

/** Per-player Bukkit scoreboard. Folia does not support this and throws UnsupportedOperationException. */
final class BukkitSidebar implements Sidebar {

    // 16 unique dummy entries, one per line slot.
    // Actual text lives in team prefix so updating a line only sends UpdateTeams packet (no flicker).
    private static final String[] ENTRIES = {
        "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
        "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };

    private final Player player;
    private final Scoreboard sb;
    private final Objective obj;

    BukkitSidebar(Player player) {
        this.player = player;
        this.sb = Bukkit.getScoreboardManager().getNewScoreboard();
        this.obj = sb.registerNewObjective("vanishpp", Criteria.DUMMY, Component.empty());
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        for (String entry : ENTRIES) {
            Team team = sb.registerNewTeam("vpp_" + entry.charAt(1));
            team.addEntry(entry);
        }
    }

    @Override
    public void render(Component title, List<Component> lines) {
        obj.displayName(title);

        int maxLines = Math.min(lines.size(), ENTRIES.length);
        for (int i = 0; i < ENTRIES.length; i++) {
            String entry = ENTRIES[i];
            Team team = sb.getTeam("vpp_" + entry.charAt(1));
            if (team == null) continue;

            if (i < maxLines) {
                team.prefix(lines.get(i));
                team.suffix(Component.empty());

                Score score = obj.getScore(entry);
                score.setScore(maxLines - i);
                try {
                    score.numberFormat(NumberFormat.blank()); // hide the score numbers entirely
                } catch (Exception ignored) {
                    // MockBukkit does not implement numberFormat(), so degrade gracefully
                }
            } else {
                sb.resetScores(entry);
            }
        }

        if (player.getScoreboard() != sb) player.setScoreboard(sb);
    }

    @Override
    public void close() {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
