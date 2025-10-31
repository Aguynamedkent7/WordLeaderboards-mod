package com.aguynamedkent.sharedjukebox;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import static net.minecraft.server.command.CommandManager.*;

public class WordTrackerCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Change permission levels to 0 (all players) for word management:
        dispatcher.register(literal("addword")
                .requires(source -> source.hasPermissionLevel(0)) // ALL players can add words!
                .then(argument("word", StringArgumentType.word())
                        .executes(WordTrackerCommands::addTrackedWord))
        );

        dispatcher.register(literal("removeword")
                .requires(source -> source.hasPermissionLevel(0)) // ALL players can remove words!
                .then(argument("word", StringArgumentType.word())
                        .executes(WordTrackerCommands::removeTrackedWord))
        );

        // Keep stats commands public too:
        dispatcher.register(literal("wordstats")
                .requires(source -> source.hasPermissionLevel(0))
                .then(argument("word", StringArgumentType.word())
                        .executes(WordTrackerCommands::showWordStats))
        );

        dispatcher.register(literal("trackedwords")
                .requires(source -> source.hasPermissionLevel(0))
                .executes(WordTrackerCommands::showTrackedWords)  // REMOVED extra ); and fixed this line
        );

        dispatcher.register(literal("wordprogress")
                .requires(source -> source.hasPermissionLevel(0))
                .then(argument("word", StringArgumentType.word())
                        .executes(WordTrackerCommands::showWordProgress))
        );
    }

    private static int showWordStats(CommandContext<ServerCommandSource> context) {
        String word = StringArgumentType.getString(context, "word");
        ServerCommandSource source = context.getSource();

        Map<UUID, Integer> stats = WordLeaderboards.wordTracker.getWordStats(word);  // FIXED: WordLeaderboards

        if (stats.isEmpty()) {
            source.sendMessage(Text.literal("❌ No stats for '" + word + "'").formatted(Formatting.RED));
            return 0;
        }

        // Create leaderboard
        source.sendMessage(Text.literal("🏆 " + word.toUpperCase() + " Leaderboard:").formatted(Formatting.GOLD));

        stats.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(entry.getKey());
                    String playerName = player != null ? player.getNameForScoreboard() : "Unknown";
                    int count = entry.getValue();
                    String medal = getMedal(stats, entry.getKey());
                    source.sendMessage(Text.literal(medal + " " + playerName + ": " + count + " times").formatted(Formatting.WHITE));
                });

        return 1;
    }

    private static int showTrackedWords(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        source.sendMessage(Text.literal("📝 Tracked Funny Words:").formatted(Formatting.AQUA));

        Set<String> words = WordLeaderboards.wordTracker.getTrackedWords();  // FIXED: WordLeaderboards
        StringBuilder wordList = new StringBuilder();
        int count = 0;

        for (String word : words) {
            wordList.append(word).append(", ");
            count++;
            if (count % 5 == 0) wordList.append("\n");
        }

        source.sendMessage(Text.literal(wordList.toString()).formatted(Formatting.WHITE));
        source.sendMessage(Text.literal("Total: " + words.size() + " words").formatted(Formatting.GRAY));

        return 1;
    }

    // ADD THESE MISSING METHODS:
    private static int addTrackedWord(CommandContext<ServerCommandSource> context) {
        String word = StringArgumentType.getString(context, "word");
        ServerCommandSource source = context.getSource();

        boolean added = WordLeaderboards.wordTracker.addTrackedWord(word);  // FIXED: WordLeaderboards

        if (added) {
            source.sendMessage(Text.literal("✅ Added '" + word + "' to tracked words!").formatted(Formatting.GREEN));
        } else {
            source.sendMessage(Text.literal("❌ '" + word + "' is already being tracked!").formatted(Formatting.RED));
        }

        return 1;
    }

    private static int removeTrackedWord(CommandContext<ServerCommandSource> context) {
        String word = StringArgumentType.getString(context, "word");
        ServerCommandSource source = context.getSource();

        boolean removed = WordLeaderboards.wordTracker.removeTrackedWord(word);  // FIXED: WordLeaderboards

        if (removed) {
            source.sendMessage(Text.literal("✅ Removed '" + word + "' from tracked words!").formatted(Formatting.GREEN));
        } else {
            source.sendMessage(Text.literal("❌ '" + word + "' is not being tracked!").formatted(Formatting.RED));
        }

        return 1;
    }

    private static String getMedal(Map<UUID, Integer> stats, UUID playerId) {
        int rank = getRank(stats, playerId);
        switch (rank) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return "▫️";
        }
    }

    private static int getRank(Map<UUID, Integer> stats, UUID playerId) {
        int playerCount = stats.get(playerId);
        int rank = 1;

        for (int count : stats.values()) {
            if (count > playerCount) rank++;
        }

        return rank;
    }

    private static int showWordProgress(CommandContext<ServerCommandSource> context) {
        String word = StringArgumentType.getString(context, "word");
        ServerCommandSource source = context.getSource();
        UUID playerId = source.getPlayer().getUuid();

        String progress = WordLeaderboards.wordTracker.getPlayerMilestoneProgress(playerId, word);  // FIXED: WordLeaderboards
        source.sendMessage(Text.literal(progress).formatted(Formatting.AQUA));

        return 1;
    }
}