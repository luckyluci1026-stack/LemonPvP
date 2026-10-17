package dev.lemonpvp.pvp.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * The sidebar of one player. Each line is a score entry ("l0", "l1", ...) whose visible
 * text is set with {@link Score#customName}; the numbers on the right are hidden.
 */
final class Board {

    /** Minecraft shows at most 15 sidebar lines. */
    static final int MAX_LINES = 15;

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private int shownLines;

    Board(Server server, Player player) {
        this.player = player;
        this.scoreboard = server.getScoreboardManager().getNewScoreboard();
        this.objective = this.scoreboard.registerNewObjective("lemonpvp", Criteria.DUMMY, Component.empty());
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.objective.numberFormat(NumberFormat.blank());
        player.setScoreboard(this.scoreboard);
    }

    void update(Component title, List<Component> lines) {
        this.objective.displayName(title);
        int count = Math.min(lines.size(), MAX_LINES);
        for (int i = 0; i < count; i++) {
            Score score = this.objective.getScore("l" + i);
            score.setScore(count - i); // higher scores are shown further up
            score.customName(lines.get(i));
        }
        for (int i = count; i < this.shownLines; i++) {
            this.objective.getScore("l" + i).resetScore();
        }
        this.shownLines = count;
    }

    /** Gives the player the server's main scoreboard back (if they still see this one). */
    void remove(Scoreboard main) {
        if (this.player.isOnline() && this.player.getScoreboard() == this.scoreboard) {
            this.player.setScoreboard(main);
        }
    }
}
