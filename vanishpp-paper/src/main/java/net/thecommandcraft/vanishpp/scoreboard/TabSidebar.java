package net.thecommandcraft.vanishpp.scoreboard;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Line;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;
import java.util.UUID;

/**
 * Sidebar drawn by TAB's scoreboard feature. Works on Folia, where the Bukkit
 * scoreboard API is unavailable, and does not fight TAB for the sidebar slot.
 * Only load this class when TAB is enabled.
 */
final class TabSidebar implements Sidebar {

    // TAB lines are strings, so the biome translatable falls back to a readable English name.
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .flattener(ComponentFlattener.builder()
                    .mapper(TextComponent.class, TextComponent::content)
                    .mapper(TranslatableComponent.class, TabSidebar::translatableFallback)
                    .build())
            .build();

    private final UUID uuid;
    private ScoreboardManager manager;
    private Scoreboard board;

    TabSidebar(UUID uuid) {
        this.uuid = uuid;
    }

    static boolean available() {
        try {
            return TabAPI.getInstance().getScoreboardManager() != null;
        } catch (IllegalStateException e) {
            return false; // TAB not loaded yet
        }
    }

    @Override
    public void render(Component title, List<Component> lines) {
        String titleText = LEGACY.serialize(title);
        List<String> texts = lines.stream().map(LEGACY::serialize).toList();

        TabAPI api = TabAPI.getInstance();
        // A TAB reload replaces the manager and discards boards created through the old one.
        if (board == null || api.getScoreboardManager() != manager) {
            open(api, titleText, texts);
            return;
        }

        if (!board.getTitle().equals(titleText)) board.setTitle(titleText);
        List<Line> current = board.getLines();
        int common = Math.min(current.size(), texts.size());
        for (int i = 0; i < common; i++) {
            if (!current.get(i).getText().equals(texts.get(i))) current.get(i).setText(texts.get(i));
        }
        for (int i = current.size() - 1; i >= common; i--) board.removeLine(i);
        for (int i = common; i < texts.size(); i++) board.addLine(texts.get(i));
    }

    private void open(TabAPI api, String title, List<String> lines) {
        board = null;
        manager = api.getScoreboardManager();
        TabPlayer tabPlayer = api.getPlayer(uuid);
        if (manager == null || tabPlayer == null) return; // TAB has not loaded the player yet; retried next tick
        board = manager.createScoreboard("vanishpp-" + uuid, title, lines);
        manager.showScoreboard(tabPlayer, board);
    }

    @Override
    public void close() {
        TabAPI api = TabAPI.getInstance();
        ScoreboardManager current = api.getScoreboardManager();
        TabPlayer tabPlayer = api.getPlayer(uuid);
        if (current != null && tabPlayer != null) current.resetScoreboard(tabPlayer);
    }

    private static String translatableFallback(TranslatableComponent component) {
        String key = component.key();
        String[] words = key.substring(key.lastIndexOf('.') + 1).split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
