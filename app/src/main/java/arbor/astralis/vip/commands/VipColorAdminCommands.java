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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static arbor.astralis.vip.commands.CommandHelper.formatChannelReference;
import static arbor.astralis.vip.commands.CommandHelper.formatRoleReference;

public final class VipColorAdminCommands implements ApplicationCommand {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final String SUBCOMMAND_ADD = "add";
    private static final String SUBCOMMAND_REMOVE = "remove";
    private static final String SUBCOMMAND_CLEAR = "clear";
    private static final String SUBCOMMAND_INFO = "info";

    private static final String PARAMETER_ROLE = "role";
    
    
    @Override
    public String getName() {
        return NAME_PREFIX + "color-admin";
    }

    @Override
    public String getShortDescription() {
        return "Configure the VIP colors for this server";
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
        var subcommandAdd = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_ADD)
            .description("Add a VIP color role")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(8)
                    .name(PARAMETER_ROLE)
                    .description("Color role to add")
                    .required(true)
                    .build()
            )
            .build();

        var subcommandRemove = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_REMOVE)
            .description("Remove a VIP color role")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .type(8)
                    .name(PARAMETER_ROLE)
                    .description("Color role to remove")
                    .required(true)
                    .build()
            )
            .build();

        var subcommandClear = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_CLEAR)
            .description("Clears all VIP color roles")
            .build();

        var subcommandInfo = ApplicationCommandOptionData.builder()
            .type(1)
            .name(SUBCOMMAND_INFO)
            .description("List all VIP color roles")
            .build();

        request.addAllOptions(
            List.of(
                subcommandAdd,
                subcommandRemove,
                subcommandClear,
                subcommandInfo
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

        if (subcommandLayerOptions.containsKey(SUBCOMMAND_ADD)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_ADD).getOptions().get(0);
            return handleAddColor(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_REMOVE)) {
            var data = subcommandLayerOptions.get(SUBCOMMAND_REMOVE).getOptions().get(0);
            return handleRemoveColor(data, event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_CLEAR)) {
            return handleClearColor(event, guildId.get().asLong());
        } else if (subcommandLayerOptions.containsKey(SUBCOMMAND_INFO)) {
            return handleInfo(event, guildId.get().asLong());
        } else {
            LOGGER.warn("Unknown subcommand for " + getName() + ": " + subcommandLayerOptions.keySet().stream().collect(Collectors.joining(", ", "[", "]")));
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }
    }

    private Mono<?> handleAddColor(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_ROLE.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Long newValue = CommandHelper.extractLongValue(data);
        boolean added = false;
        
        if (newValue != null) {
            GuildSettings settings = Settings.forGuild(guildId);
            List<Long> vipColorRoleIds = settings.getVipColorRoleIds();
            
            if (!vipColorRoleIds.contains(newValue)) {
                vipColorRoleIds.add(newValue);
                Settings.persistForGuild(settings);
                
                added = true;
            }
        }

        if (added) {
            return event.reply()
                .withContent("Done deal! " + formatRoleReference(newValue) + " is now added as a VIP color~");
        } else {
            return event.reply()
                .withContent("Haha, " + formatRoleReference(newValue) + " is already a VIP color, no need to add twice.");
        }
    }

    private Mono<?> handleRemoveColor(ApplicationCommandInteractionOptionData data, ApplicationCommandInteractionEvent event, long guildId) {
        if (!PARAMETER_ROLE.equals(data.name())) {
            LOGGER.warn("Unexpected parameter name: " + data.name());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Long newValue = CommandHelper.extractLongValue(data);
        boolean removed = false;
        
        if (newValue != null) {
            GuildSettings settings = Settings.forGuild(guildId);
            List<Long> vipColorRoleIds = settings.getVipColorRoleIds();
            removed = vipColorRoleIds.remove(newValue);

            Settings.persistForGuild(settings);
        }

        if (removed) {
            return event.reply()
                .withContent("Done deal! " + formatRoleReference(newValue) + " is no longer a VIP color");
        } else {
            return event.reply()
                .withContent("Uh-oh, the role " + formatRoleReference(newValue) + " was never a VIP color! No worries.");
        }
    }

    private Mono<?> handleClearColor(ApplicationCommandInteractionEvent event, long guildId) {
        GuildSettings settings = Settings.forGuild(guildId);
        settings.setVipColorRoleIds(new ArrayList<>(1));

        Settings.persistForGuild(settings);

        return event.reply()
            .withContent("All sorted! All VIP colors have been erased.");
    }

    private Mono<?> handleInfo(ApplicationCommandInteractionEvent event, long guildId) {
        GuildSettings settings = Settings.forGuild(guildId);

        List<Long> vipColorRoleIds = settings.getVipColorRoleIds();
        
        if (vipColorRoleIds == null) {
            vipColorRoleIds = new ArrayList<>(1);
        }

        var contentBuilder = new StringBuilder();
        
        for (Long roleId : vipColorRoleIds) {
            if (roleId == null) {
                continue;
            }
            
            contentBuilder.append(CommandHelper.formatRoleReference(roleId)).append("\n");
        }
        
        var embedSpec = EmbedCreateSpec.builder()
            .description("**VIP Colors**\n\n" + (vipColorRoleIds.isEmpty() ? "_(none)_" : contentBuilder))
            .color(Color.BISMARK)
            .build();

        return event.reply()
            .withEmbeds(embedSpec);
    }
}
