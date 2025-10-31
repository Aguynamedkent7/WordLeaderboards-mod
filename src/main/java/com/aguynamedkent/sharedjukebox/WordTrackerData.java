package com.aguynamedkent.sharedjukebox;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.util.*;

public class WordTrackerData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DATA_FILE = new File("config/word_tracker_data.json");

    private static final int[] MILESTONES = {10, 50, 100, 500, 1000};

    // EMPTY starting list - players will add words
    private Set<String> funnyWords = new HashSet<>();

    public Map<String, Map<String, Integer>> wordStats = new HashMap<>(); // UUID as String for JSON
    public Map<String, Set<String>> playerMilestones = new HashMap<>();   // UUID as String for JSON

    private String normalizeWord(String word) {
        if (word == null || word.isEmpty()) return word;
        word = word.toLowerCase();
        StringBuilder normalized = new StringBuilder();
        char lastChar = '\0';

        for (char c : word.toCharArray()) {
            if (c != lastChar) {
                normalized.append(c);
                lastChar = c;
            }
        }
        return normalized.toString();
    }

    public String trackWord(String rawWord, UUID playerId, String playerName) {
        String normalizedWord = normalizeWord(rawWord);
        String playerIdStr = playerId.toString();

        if (funnyWords.contains(normalizedWord)) {
            // Get current count and increment
            int currentCount = wordStats
                    .computeIfAbsent(normalizedWord, k -> new HashMap<>())
                    .merge(playerIdStr, 1, Integer::sum);

            // Save after every update
            save();

            // Check for milestone
            String milestoneMessage = checkMilestone(playerIdStr, normalizedWord, currentCount, playerName);
            if (milestoneMessage != null) {
                return milestoneMessage;
            }
        }
        return null;
    }

    private String checkMilestone(String playerId, String word, int currentCount, String playerName) {
        for (int milestone : MILESTONES) {
            if (currentCount == milestone) {
                // Track that this player reached this milestone for this word
                playerMilestones.computeIfAbsent(playerId, k -> new HashSet<>())
                        .add(word + ":" + milestone);

                save(); // Save when milestone is reached
                return getMilestoneMessage(playerName, word, milestone);
            }
        }
        return null;
    }

    private String getMilestoneMessage(String playerName, String word, int milestone) {
        switch (milestone) {
            case 10: return "🎉 " + playerName + " just said '" + word + "' for the 10th time!";
            case 50: return "🔥 " + playerName + " has said '" + word + "' 50 times! Getting addicted!";
            case 100: return "🏆 " + playerName + " reached 100 '" + word + "'s! Legendary!";
            case 500: return "👑 " + playerName + " has said '" + word + "' 500 times! Absolute madlad!";
            case 1000: return "💀 " + playerName + " JUST SAID '" + word + "' FOR THE 1000TH TIME! TOUCH GRASS!";
            default: return "🎯 " + playerName + " reached a new '" + word + "' milestone!";
        }
    }

    public Map<UUID, Integer> getWordStats(String word) {
        String normalizedWord = normalizeWord(word);
        Map<String, Integer> stringStats = wordStats.getOrDefault(normalizedWord, new HashMap<>());

        // Convert String UUIDs back to UUID objects
        Map<UUID, Integer> uuidStats = new HashMap<>();
        for (Map.Entry<String, Integer> entry : stringStats.entrySet()) {
            uuidStats.put(UUID.fromString(entry.getKey()), entry.getValue());
        }
        return uuidStats;
    }

    public Set<String> getTrackedWords() {
        return new HashSet<>(funnyWords);
    }

    // Word management - available to all players
    public boolean addTrackedWord(String word) {
        String normalizedWord = normalizeWord(word);
        if (funnyWords.contains(normalizedWord)) {
            return false; // Word already exists
        }

        funnyWords.add(normalizedWord);
        save();
        return true;
    }

    public boolean removeTrackedWord(String word) {
        String normalizedWord = normalizeWord(word);
        boolean removed = funnyWords.remove(normalizedWord);
        if (removed) {
            save();
        }
        return removed;
    }

    // Get player's milestone progress for a word
    public String getPlayerMilestoneProgress(UUID playerId, String word) {
        Map<UUID, Integer> stats = getWordStats(word);
        int count = stats.getOrDefault(playerId, 0);

        StringBuilder progress = new StringBuilder();
        progress.append("Progress for '").append(word).append("': ").append(count).append("/1000\n");
        progress.append("Milestones: ");

        for (int milestone : MILESTONES) {
            boolean reached = count >= milestone;
            progress.append(reached ? "✅" : "◻️").append(milestone).append(" ");
        }

        return progress.toString();
    }

    public void save() {
        try {
            DATA_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save word tracker data: " + e.getMessage());
        }
    }

    public static WordTrackerData load() {
        if (!DATA_FILE.exists()) {
            WordTrackerData newData = new WordTrackerData();
            newData.save();
            return newData;
        }

        try (FileReader reader = new FileReader(DATA_FILE)) {
            return GSON.fromJson(reader, WordTrackerData.class);
        } catch (IOException e) {
            System.err.println("Failed to load word tracker data: " + e.getMessage());
            return new WordTrackerData();
        }
    }
}