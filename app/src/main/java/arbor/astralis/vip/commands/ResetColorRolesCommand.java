package arbor.astralis.vip.commands;

import arbor.astralis.vip.Branding;
import arbor.astralis.vip.GuildSettings;
import arbor.astralis.vip.Settings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

public final class ResetColorRolesCommand implements ApplicationCommand {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public String getName() {
        return NAME_PREFIX + "reset-colors";
    }

    @Override
    public String getShortDescription() {
        return "Removes all your VIPs color roles";
    }

    @Override
    public Optional<String> getParametersHelpText() {
        return Optional.empty();
    }

    @Override
    public void create(ImmutableApplicationCommandRequest.Builder request) {

    }

    @Override
    public Mono<?> onInteraction(ApplicationCommandInteractionEvent event) {
        Interaction interaction = event.getInteraction();

        Optional<Snowflake> guildIdValue = interaction.getGuildId();
        if (guildIdValue.isEmpty()) {
            LOGGER.warn("Missing guildId for interaction: {}", interaction.getId());
            return Mono.empty();
        }

        long guildId = guildIdValue.get().asLong();
        GuildSettings guildSettings = Settings.forGuild(guildId);

        List<Long> vipColorRoles = guildSettings.getVipColorRoleIds();
        List<Long> rolesToRemove = new ArrayList<>();

        Member member = interaction.getUser().asMember(interaction.getGuildId().get()).block();
        
        if (member != null) {
            Flux<Role> rolesFlux = member.getRoles();

            rolesFlux.doOnEach(roleSignal -> {
                Role role = roleSignal.get();

                if (role == null) {
                    return;
                }

                long roleId = role.getId().asLong();

                if (vipColorRoles.contains(roleId)) {
                    rolesToRemove.add(roleId);
                }

                if (!rolesToRemove.isEmpty()) {
                    for (Long roleToRemove : rolesToRemove) {
                        member.removeRole(Snowflake.of(roleToRemove), "Member requested removal (by " + Branding.BOT_NAME + ")").block();
                    }
                }
            }).blockLast();
        }
        
        return event.reply()
            .withContent(Branding.getResetColorRolesSuccessMessage(rolesToRemove));
    }

}
