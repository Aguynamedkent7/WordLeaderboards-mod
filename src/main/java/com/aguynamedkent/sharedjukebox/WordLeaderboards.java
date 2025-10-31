package com.aguynamedkent.sharedjukebox;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordLeaderboards implements ModInitializer {
    public static final String MOD_ID = "WordLeaderboards";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static WordTrackerData wordTracker;

    @Override
    public void onInitialize() {
        LOGGER.info("Funny Word Tracker mod initializing!");
        wordTracker = WordTrackerData.load();
        LOGGER.info("Loaded word tracker data from file");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WordTrackerCommands.register(dispatcher);
        });

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String rawMessage = message.getContent().getString();
            String[] words = rawMessage.split("\\s+");

            for (String word : words) {
                word = word.replaceAll("[^a-zA-Z]", "").toLowerCase();
                if (!word.isEmpty()) {
                    String reaction = wordTracker.trackWord(word, sender.getUuid(), sender.getNameForScoreboard());
                    if (reaction != null) {
                        sender.sendMessage(Text.literal(reaction));
                    }
                }
            }
        });
    }
}