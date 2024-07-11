package arbor.astralis.vip.commands;

import arbor.astralis.vip.Branding;
import arbor.astralis.vip.GuildSettings;
import arbor.astralis.vip.PerkManager;
import arbor.astralis.vip.PremiumTier;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SetTierCommand implements ApplicationCommand {

    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final String PARAMETER_USER = "vip_member";
    private static final String PARAMETER_TIER = "tier";
    
    private static final int TIER_1 = PremiumTier.TIER_1.getOrdinal();
    private static final int TIER_2 = PremiumTier.TIER_2.getOrdinal();
    private static final int TIER_3 = PremiumTier.TIER_3.getOrdinal();
    
    
    @Override
    public String getName() {
        return NAME_PREFIX + "set-tier";
    }

    @Override
    public String getShortDescription() {
        return "Sets a VIP member's current tier";
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.of("<@userId> [1|2|3]");
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {
        var vipMemberOption = ApplicationCommandOptionData.builder()
            .type(6)
            .name(PARAMETER_USER)
            .description("The VIP member to be updated")
            .required(true)
            .build();

        var tierOption = ApplicationCommandOptionData.builder()
            .type(4)
            .name(PARAMETER_TIER)
            .description("The VIP tier to set")
            .choices(
                List.of(
                    ApplicationCommandOptionChoiceData.builder().name("Tier 1 (vip - gold)").value(TIER_1).build(),
                    ApplicationCommandOptionChoiceData.builder().name("Tier 2 (vip - platinum)").value(TIER_2).build(),
                    ApplicationCommandOptionChoiceData.builder().name("Tier 3 (vip - deluxe)").value(TIER_3).build()
                )
            )
            .required(true)
            .build();

        request.addOption(vipMemberOption);
        request.addOption(tierOption);
    }

    @Override
    public Mono<?> onInteraction(ApplicationCommandInteractionEvent event) {
        if (!CommandHelper.isTriggerUserAdmin(event)) {
            return event.reply().withContent(Branding.getAdminOnlyCommandMessage());
        }
        
        Optional<Snowflake> guildId = event.getInteraction().getGuildId();
        if (guildId.isEmpty()) {
            LOGGER.warn("No guildId for interaction");
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }
        
        Map<String, CommandHelper.OptionsAndValues> optionsAndValues = CommandHelper.marshalOptionValues(event);

        CommandHelper.OptionsAndValues userValues = optionsAndValues.get(PARAMETER_USER);
        CommandHelper.OptionsAndValues tierValues = optionsAndValues.get(PARAMETER_TIER);
        
        if (userValues == null || tierValues == null || userValues.getValue() == null || tierValues.getValue() == null) {
            LOGGER.warn("Unexpected interaction response: userValues=" + (userValues == null) + ", tierValues=" + (tierValues == null));
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        long userId;
        int tier;
        
        try {
            userId = Long.parseLong(userValues.getValue());
            tier = Integer.parseInt(tierValues.getValue());
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to parse user or tier value: " + userValues.getValue() + ", " + tierValues.getValue());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        Optional<PremiumTier> premiumTier = PremiumTier.fromOrdinal(tier);
        
        if (premiumTier.isEmpty()) {
            LOGGER.warn("No premium tier for value: " + tierValues.getValue());
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }

        GatewayDiscordClient client = event.getClient();
        Guild guild = client.getGuildById(guildId.get()).block();
        if (guild == null) {
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }
        
        Member member = guild.getMemberById(Snowflake.of(userId)).block();
        if (member == null) {
            return event.reply().withContent(Branding.getUnexpectedErrorMessage());
        }
        if (member.getPremiumTime().isEmpty()) {
            LOGGER.warn("No premium tier for value: " + tierValues.getValue());
            return event.reply().withContent("Hold it partner, that member is not a server booster!");
        }
        
        long triggerUserId = event.getInteraction().getUser().getId().asLong();
        boolean success = PerkManager.setPerkForMember(member, guild, premiumTier.get(), false, client, null, triggerUserId, true);

        return event.reply()
            .withContent(
                success ? "Successfully updated <@" + userId + "> to " + premiumTier.get() + "! :rose:" 
                    : "Failed to update <@" + userId + ">"
            );
    }
}
