package arbor.astralis.vip;

import arbor.astralis.vip.commands.ApplicationCommand;
import arbor.astralis.vip.commands.ApplicationCommands;
import arbor.astralis.vip.commands.CommandHelper;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

public final class Main {
    
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws InterruptedException {
        Optional<String> botToken = Optional.empty();

        try {
            botToken = Settings.initialize();
        } catch (IOException e) {
            LOGGER.fatal("Failed to initialize settings directory!", e);
            System.exit(1);
        }

        if (botToken.isEmpty()) {
            LOGGER.error("Bot token file is missing!");
            System.exit(1);
        }

        DiscordClient client = DiscordClient.create(botToken.get());
        Long applicationId = client.getApplicationId().block();

        if (applicationId == null) {
            LOGGER.error("Failed to retrieve applicationId!");
            System.exit(1);
        }

        createApplicationCommands(client, applicationId);

        client.gateway()
            .setEnabledIntents(IntentSet.nonPrivileged().or(IntentSet.of(Intent.GUILD_MEMBERS)))
            .withGateway(Main::initializeGateway)
            .block();
    }

    private static void createApplicationCommands(DiscordClient client, long applicationId) {
        int totalCreated = 0;

//        resetAllCommands(client, applicationId);

        for (ApplicationCommand command : ApplicationCommands.getAll()) {
            var commandBuilder = ApplicationCommandRequest.builder()
                .name(command.getName())
                .description(command.getShortDescription());

            command.create(commandBuilder);

            ApplicationCommandRequest request = commandBuilder.build();

            client.getApplicationService().createGlobalApplicationCommand(applicationId, request).subscribe();

            totalCreated++;
            LOGGER.info("Created command: " + request.name());
        }

        LOGGER.info("Loaded commands: " + totalCreated);
    }

    private static void resetAllCommands(DiscordClient client, long applicationId) {
        client.getApplicationService().getGlobalApplicationCommands(applicationId)
            .doOnEach(command -> {
                if (command.get() == null) return;
                
                Id commandId = command.get().id();
                client.getApplicationService().deleteGlobalApplicationCommand(applicationId, commandId.asLong());
                LOGGER.info("Deleted global command: " + command.get().name() + " (id: " + commandId + ")");
            }).subscribe();

        client.getApplicationService().getGuildApplicationCommands(applicationId, 1210865909321957376L)
            .doOnEach(command -> {
                if (command.get() == null) return;
                
                Id commandId = command.get().id();
                client.getApplicationService().deleteGuildApplicationCommand(applicationId, 1210865909321957376L, commandId.asLong());
                LOGGER.info("Deleted guild command: " + command.get().name() + " (id: " + commandId + ")");
            }).subscribe();
    }

    private static Mono<?> initializeGateway(GatewayDiscordClient client) {
        
        PerkRefresher.initialize(client);
        PerkManager.refreshPerksForAllGuilds(client);
        
        client.on(ApplicationCommandInteractionEvent.class, event -> {
            String name = event.getCommandName();
            Optional<ApplicationCommand> command = ApplicationCommands.forName(name);

            if (command.isEmpty()) {
                LOGGER.warn("No command mapping for name: " + name);
            } else {
                return command.get().onInteraction(event);
            }

            return Mono.empty();
        }).subscribe();

        client.on(ButtonInteractionEvent.class, event -> {
            String[] payload = CommandHelper.parseCommandButtonPayload(event.getCustomId());

            if (payload.length >= 1) {
                String commandSource = payload[0];
                Optional<ApplicationCommand> command = ApplicationCommands.forName(commandSource);

                if (command.isEmpty()) {
                    LOGGER.warn("Invalid command source for ButtonInteraction event: " + commandSource);
                    return Mono.empty();
                }

                return command.get().onButtonInteraction(payload, event);
            }

            LOGGER.warn("Unrecognized button payload: " + event.getCustomId());
            return Mono.empty();
        }).subscribe();

        client.on(MemberUpdateEvent.class, event -> {
            Optional<Instant> premiumSince = event.getCurrentPremiumSince();

            if (premiumSince.isPresent()) {
                Member member = event.getMember().block();
                Guild guild = event.getGuild().block();

                PerkManager.refreshPerksForMember(member, premiumSince.get(), guild, client, null);
            }

            return Mono.empty();
        }).subscribe();

        return Mono.empty();
    }

}
