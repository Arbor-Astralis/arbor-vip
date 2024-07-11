package arbor.astralis.vip.commands;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public final class CommandHelper {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PAYLOAD_SEPARATOR = ";";

    private CommandHelper() {
        
    }
    
    public static Map<String, OptionsAndValues> marshalOptionValues(ApplicationCommandInteractionEvent event) {
        Map<String, OptionsAndValues> optionValues = new HashMap<>(2);

        event.getInteraction().getData().data().toOptional().flatMap(commandData ->
            commandData.options().toOptional()).ifPresent(options -> {
                for (ApplicationCommandInteractionOptionData option : options) {
                    var entry = new OptionsAndValues(
                        option.options().toOptional().orElse(new ArrayList<>(2)), 
                        option.value().toOptional().orElse(null)
                    );
                    
                    optionValues.put(option.name(), entry);
                }
            }
        );

        return optionValues;
    }
    
    public static final class OptionsAndValues {
        
        private final List<ApplicationCommandInteractionOptionData> options;
        private final @Nullable String value;
        
        public OptionsAndValues(
            List<ApplicationCommandInteractionOptionData> options, 
            @Nullable String value
        ) {
            this.options = options;
            this.value = value;
        }

        public List<ApplicationCommandInteractionOptionData> getOptions() {
            return options;
        }

        @Nullable
        public String getValue() {
            return value;
        }
    }

    public static boolean isTriggerUserAdmin(ApplicationCommandInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        
        if (guild == null) {
            return false;
        }
        
        var commandUser = event.getInteraction().getUser();
        Member guildMember = guild.getMemberById(commandUser.getId()).block();
        
        if (guildMember == null) {
            return false;
        }
        
        PermissionSet permissions = guildMember.getBasePermissions().block();
        if (permissions == null) {
            return false;
        }
        
        return permissions.contains(Permission.ADMINISTRATOR);
    }

    public static String createCommandButtonPayload(ApplicationCommand command, Object ... data) {
        StringBuilder payload = new StringBuilder(command.getName());
        
        if (data.length > 0) {
            payload.append(PAYLOAD_SEPARATOR);
        }

        for (int i = 0; i < data.length; i++) {
            payload.append(escapePayloadValue(String.valueOf(data[i])));
            
            if (i != data.length - 1) {
                payload.append(PAYLOAD_SEPARATOR);
            }
        }
        
        return payload.toString();
    }
    
    public static String[] parseCommandButtonPayload(String payload) {
        String[] dataParts = payload.split(PAYLOAD_SEPARATOR);

        for (int i = 0; i < dataParts.length; i++) {
            dataParts[i] = unescapePayloadValue(dataParts[i]);
        }
        
        return dataParts;
    }

    private static String unescapePayloadValue(String entry) {
        return entry.replaceAll("\\\\" + PAYLOAD_SEPARATOR, PAYLOAD_SEPARATOR);
    }

    private static String escapePayloadValue(String entry) {
        return entry.replaceAll(PAYLOAD_SEPARATOR, "\\\\" + PAYLOAD_SEPARATOR);
    }

    public static Long extractLongValue(ApplicationCommandInteractionOptionData data) {
        return data.value().toOptional().flatMap(stringValue -> {
            try {
                long parsedLong = Long.parseLong(stringValue);
                return Optional.of(parsedLong);
            } catch (NumberFormatException e) {
                LOGGER.warn("Unable to parse String value to long: " + stringValue);
                return Optional.empty();
            }
        }).orElse(null);
    }

    public static String formatRoleReference(@Nullable Long roleId) {
        return roleId == null ? "_(unset)_" : "<@&" + roleId + ">";
    }

    public static String formatChannelReference(@Nullable Long channelId) {
        return channelId == null ? "_(unset)_" : "<#" + channelId + ">";
    }
}
