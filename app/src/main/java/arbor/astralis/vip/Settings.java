package arbor.astralis.vip;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Scanner;

public final class Settings {
    
    private static final String BOT_SETTINGS_FOLDER_NAME = "bot-data";
    private static final String BOT_TOKEN_FILE_NAME = "bot-token";
    
    private Settings() {
        
    }
    
    public static Optional<String> initialize() throws IOException {
        resolveDataFolder();
        
        return loadBotToken();
    }

    private static void resolveDataFolder() throws IOException {
        Path settingsFolder = getDataFolder();
        if (!Files.isDirectory(settingsFolder)) {
            Files.createDirectories(settingsFolder);
        }
    }

    private static Optional<String> loadBotToken() throws IOException {
        Path tokenFile = getDataFolder().resolve(BOT_TOKEN_FILE_NAME);
        
        if (!Files.exists(tokenFile)) {
            Files.createFile(tokenFile);
            return Optional.empty();
        }
        
        String token = "";
        
        try (Scanner scanner = new Scanner(Files.newBufferedReader(tokenFile))) {
            if (scanner.hasNextLine()) {
                token = scanner.nextLine().replace("\n", "");
            }
        }
        
        if (token.isBlank()) {
            return Optional.empty();
        }
        
        return Optional.of(token);
    }
    
    private static Path getDataFolder() {
        return Paths.get(System.getProperty("user.dir"), BOT_SETTINGS_FOLDER_NAME);
    }
    
    public static GuildSettings forGuild(long guildId) {
        
        Path settingsFile = getGuildSettingsFile(guildId);
        var persistedSettings = new GuildSettingsDocument();
        
        if (Files.exists(settingsFile)) {
            var mapper = new ObjectMapper();
            try {
                persistedSettings = mapper.readValue(settingsFile.toFile(), GuildSettingsDocument.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        return getGuildSettings(guildId, persistedSettings);
    }

    private static GuildSettings getGuildSettings(long guildId, GuildSettingsDocument document) {
        var settings = new GuildSettings(guildId);
        
        settings.setVipColorRoleIds(document.getVipColorRoleIds());
        settings.setVipTier1RoleId(document.getVipTier1RoleId());
        settings.setVipTier2RoleId(document.getVipTier2RoleId());
        settings.setVipTier3RoleId(document.getVipTier3RoleId());
        settings.setVipHonorRoleId(document.getVipHonorRoleId());
        settings.setBroadcastChannelId(document.getBroadcastChannelId());
        settings.setModChannelId(document.getModChannelId());
        
        return settings;
    }

    private static Path resolveGuildDataDirectory(long guildId) {
        Path guildDataDirectory = getDataFolder().resolve(String.valueOf(guildId));
        
        if (!Files.isDirectory(guildDataDirectory)) {
            try {
                Files.createDirectories(guildDataDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        return guildDataDirectory;
    }

    public static void persistForGuild(GuildSettings guildSettings) {
        Path settingsFile = getGuildSettingsFile(guildSettings.getGuildId());

        var document = createGuildSettingsDocument(guildSettings);

        var mapper = new ObjectMapper();
        try {
            String serializedData = mapper.writeValueAsString(document);
            
            Files.deleteIfExists(settingsFile);
            
            try(BufferedWriter writer = Files.newBufferedWriter(settingsFile)) {
                writer.write(serializedData);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static GuildSettingsDocument createGuildSettingsDocument(GuildSettings guildSettings) {
        var document = new GuildSettingsDocument();
        
        document.setVipColorRoleIds(guildSettings.getVipColorRoleIds());
        document.setVipTier1RoleId(guildSettings.getVipTier1RoleId());
        document.setVipTier2RoleId(guildSettings.getVipTier2RoleId());
        document.setVipTier3RoleId(guildSettings.getVipTier3RoleId());
        document.setVipHonorRoleId(guildSettings.getVipHonorRoleId());
        document.setBroadcastChannelId(guildSettings.getBroadcastChannelId());
        document.setModChannelId(guildSettings.getModChannelId());
        
        return document;
    }

    private static Path getGuildSettingsFile(long guildId) {
        return resolveGuildDataDirectory(guildId).resolve("settings.json");
    }
}
