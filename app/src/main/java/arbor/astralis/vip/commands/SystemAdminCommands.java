package arbor.astralis.vip.commands;

import arbor.astralis.vip.Branding;
import arbor.astralis.vip.GuildSettings;
import arbor.astralis.vip.Settings;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static arbor.astralis.vip.commands.CommandHelper.formatChannelReference;
import static arbor.astralis.vip.commands.CommandHelper.formatRoleReference;

public final class SystemAdminCommands implements ApplicationCommand {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final String SUBCOMMAND_T1 = "set-tier1";
    private static final String SUBCOMMAND_T2 = "set-tier2";
    private static final String SUBCOMMAND_T3 = "set-tier3";
    private static final String SUBCOMMAND_HONOR = "set-honor";
    private static final String SUBCOMMAND_BROADCAST_CHANNEL = "set-broadcast-channel";
    private static final String SUBCOMMAND_MOD_CHANNEL = "set-mod-channel";
    private static final String SUBCOMMAND_INFO = "info";
    
    private static final String PARAMETER_ROLE = "role";
    private static final String PARAMETER_CHANNEL = "channel";
    
    
    @Override
    public String getName() {
        return NAME_PREFIX + "system-admin";
    }

    @Override
    public String getShortDescription() {
        return "Configure the VIP system for this server";
    }

    @Override
    public String getExtendedDescription() {
        return "(Admin only) " + getShortDescription();
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.empty();
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {
        var subcommandT1 = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_T1)
            .description("Link server role for Tier 1 VIP")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(8)
                    .name(PARAMETER_ROLE)
                    .description("Role to link")
                    .required(true)
                    .build()
            )
            .build();

        var subcommandT2 = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_T2)
            .description("Link server role for Tier 2 VIP")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(8)
                    .name(PARAMETER_ROLE)
                    .description("Role to link")
                    .required(true)
                    .build()
            )
            .build();

        var subcommandT3 = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_T3)
            .description("Link server role for Tier 3 VIP")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(8)
                    .name(PARAMETER_ROLE)
                    .description("Role to link")
                    .required(true)
                    .build()
            )
            .build();

        var subcommandHonors = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_HONOR)
            .description("Link server role for VIP veteran honor role")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(8)
                    .name(PARAMETER_ROLE)
                    .description("Role to link")
                    .required(true)
                    .build()
            )
            .build();

        var subcommandBroadcastChannelSet = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_BROADCAST_CHANNEL)
            .description("Set public text channel to announce perk changes to affected VIP members")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(7)
                    .name(PARAMETER_CHANNEL)
                    .description("Channel to use")
                    .required(true)
                    .build()
            )
            .build();

        var subcommandModChannelSet = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_MOD_CHANNEL)
            .description("Set mod text channel to announce periodic system checks")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(7)
                    .name(PARAMETER_CHANNEL)
                    .description("Channel to use")
                    .required(true)
                    .build()
            )
            .build();


        var subcommandList = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_INFO)
            .description("Display current setup information")
            .build();
        
        request.addAllOptions(
            List.of(
                subcommandT1, 
                subcommandT2, 
                subcommandT3, 
                subcommandHonors,
                subcommandBroadcastChannelSet,
                subcommandModChannelSet,
                subcommandList
            )
        );
    }

    @Override
    public Mono<?> onInteraction(ApplicationCommandInteractionEvent event) {
        if (!CommandHelper.isTriggerUserAdmin(event)) {
            return event.reply().withContent(Branding.getAdminOnlyCommandMessage());
        }

        var guildId = event.getInteraction().getGuildId();

        if (guildId.isEmpty()) {
            LOGGER.warn("Missing guildId for sub-command");
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        var subcommandLayerOptions = CommandHelper.marshalOptionValues(event);
        
        if (subcommandLayerOptions.containsKey(SUBCOMMAND_T1)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_T1).getOptions().get(0);
            return handleSetTier1(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_T2)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_T2).getOptions().get(0);
            return handleSetTier2(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_T3)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_T3).getOptions().get(0);
            return handleSetTier3(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_HONOR)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_HONOR).getOptions().get(0);
            return handleSetHonor(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_BROADCAST_CHANNEL)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_BROADCAST_CHANNEL).getOptions().get(0);
            return handleSetBroadcastChannel(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_MOD_CHANNEL)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_MOD_CHANNEL).getOptions().get(0);
            return handleSetModChannel(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_INFO)) {
            return handleInfo(event, guildId.get().asLong());
        } else {
            LOGGER.warn("Unknown subcommand for " + getName() + ": " + subcommandLayerOptions.keySet().stream().collect(Collectors.joining(", ", "[", "]")));
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }
    }

    private Mono<?> handleSetTier1(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_ROLE.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Long newValue = CommandHelper.extractLongValue(data);

        GuildSettings settings = Settings.forGuild(guildId);
        settings.setVipTier1RoleId(newValue);

        Settings.persistForGuild(settings);

        return event.reply()
                    .withContent("Right as rain~ VIP tier 1 role is now set to: " + formatRoleReference(newValue));
    }

    private Mono<?> handleSetTier2(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_ROLE.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Long newValue = CommandHelper.extractLongValue(data);

        GuildSettings settings = Settings.forGuild(guildId);
        settings.setVipTier2RoleId(newValue);

        Settings.persistForGuild(settings);

        return event.reply()
                    .withContent("Right as rain~ VIP tier 2 role is now set to: " + formatRoleReference(newValue));
    }

    private Mono<?> handleSetTier3(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_ROLE.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Long newValue = CommandHelper.extractLongValue(data);

        GuildSettings settings = Settings.forGuild(guildId);
        settings.setVipTier3RoleId(newValue);

        Settings.persistForGuild(settings);

        return event.reply()
                    .withContent("Right as rain~ VIP tier 3 role is now set to: " + formatRoleReference(newValue));
    }

    private Mono<?> handleSetHonor(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_ROLE.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }
        
        Long newValue = CommandHelper.extractLongValue(data);
        
        GuildSettings settings = Settings.forGuild(guildId);
        settings.setVipHonorRoleId(newValue);
        
        Settings.persistForGuild(settings);
        
        return event.reply()
                    .withContent("Done deal! VIP veteran honor role is now set to: " + formatRoleReference(newValue));
    }
    
    private Mono<?> handleSetBroadcastChannel(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_CHANNEL.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Long newValue = CommandHelper.extractLongValue(data);

        GuildSettings settings = Settings.forGuild(guildId);
        settings.setBroadcastChannelId(newValue);

        Settings.persistForGuild(settings);

        return event.reply()
            .withContent("Done deal! VIP public broadcast channel is now set to: " + formatChannelReference(newValue));
    }

    private Mono<?> handleSetModChannel(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_CHANNEL.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Long newValue = CommandHelper.extractLongValue(data);

        GuildSettings settings = Settings.forGuild(guildId);
        settings.setModChannelId(newValue);

        Settings.persistForGuild(settings);

        return event.reply()
            .withContent("Cool! VIP mod pulse check channel is now set to: " + formatChannelReference(newValue));
    }

    private Mono<?> handleInfo(ApplicationCommandInteractionEvent event, long guildId) {
        GuildSettings settings = Settings.forGuild(guildId);
        
        var embedSpec = EmbedCreateSpec.builder()
            .description(
                "**System Setup**\n\n" +
                    "Tier 1: " + formatRoleReference(settings.getVipTier1RoleId()) + "\n" +
                    "Tier 2: " + formatRoleReference(settings.getVipTier2RoleId()) + "\n" + 
                    "Tier 3: " + formatRoleReference(settings.getVipTier3RoleId()) + "\n\n" +
                    "Veteran honor: " + formatRoleReference(settings.getVipHonorRoleId()) + "\n" +
                    "\n" +
                    "Broadcasting Channel: " + formatChannelReference(settings.getBroadcastChannelId()) + "\n" + 
                    "Mod Pulse-check Channel: " + formatChannelReference(settings.getModChannelId())
            )
            .color(Color.BISMARK)
            .build();
        
        return event.reply()
            .withEmbeds(embedSpec);
    }
}
