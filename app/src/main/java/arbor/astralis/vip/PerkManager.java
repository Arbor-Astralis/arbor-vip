package arbor.astralis.vip;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class PerkManager {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    private PerkManager() {
        // helper class, no instantiation
    }
    
    public static void refreshPerksForAllGuilds(GatewayDiscordClient client) {
        List<Guild> guilds = client.getGuilds().collectList().block();
        
        for (Guild guild : guilds) {
            if (guild != null) {
                GuildSettings settings = Settings.forGuild(guild.getId().asLong());
                if (!validateGuildSettings(settings, client)) {
                    return;
                }

                refreshPerksForGuild(guild, client, settings);
            }
        }
    }

    private static void refreshPerksForGuild(Guild guild, GatewayDiscordClient client, GuildSettings settings) {
        List<Member> members = guild.getMembers().collectList().block();
        
        List<Long> vipObserved = new ArrayList<>();
        List<Long> vipUpdated = new ArrayList<>();
        
        for (Member member : members) {
            if (member == null) {
                continue;
            }

            Optional<Instant> premiumTime = member.getPremiumTime();
            if (premiumTime.isPresent()) {
                vipObserved.add(member.getId().asLong());
                
                if (refreshPerksForMember(member, premiumTime.get(), guild, client, settings)) {
                    vipUpdated.add(member.getId().asLong());
                }
            }
        }

        assert settings.getModChannelId() != null;
        
        GuildChannel modChannel = guild.getChannelById(Snowflake.of(settings.getModChannelId())).block();
        
        if (modChannel instanceof GuildMessageChannel messageChannel) {
            var vipObservedNames = new StringBuilder();
            for (Long userId : vipObserved) {
                vipObservedNames.append("<@").append(userId).append(">").append("\n");
            }

            var vipUpdatedNames = new StringBuilder();
            for (Long userId : vipUpdated) {
                vipUpdatedNames.append("<@").append(userId).append(">").append("\n");
            }
            
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .description(
                    "**Completed periodic perk refresh**\n" +
                        "\n" +
                        "VIPs observed: " + vipUpdated.size() + "\n" + vipObservedNames + "\n\n" +
                        "VIPs updated: " + vipUpdated + "\n" + vipUpdatedNames
                )
                .color(Color.BISMARK)
                .build();
            
            messageChannel.createMessage(embed).subscribe();
        }
    }

    private static boolean refreshPerksForMember(
        Member member,
        Instant premiumTime,
        Guild guild,
        GatewayDiscordClient client,
        @Nullable GuildSettings guildSettings
    ) {
        assert member != null;
        
        if (guildSettings == null) {
            guildSettings = Settings.forGuild(guild.getId().asLong());
            
            if (!validateGuildSettings(guildSettings, client)) {
                return false;
            }
        }

        assert guildSettings.getVipTier1RoleId() != null;
        assert guildSettings.getVipTier2RoleId() != null;
        assert guildSettings.getVipTier3RoleId() != null;
        assert guildSettings.getVipHonorRoleId() != null;
        assert guildSettings.getModChannelId() != null;
        
        long now = Instant.now().toEpochMilli();
        long premiumSince = premiumTime.toEpochMilli();
        long premiumDurationMillis = now - premiumSince;
        long premiumDays = TimeUnit.MILLISECONDS.toDays(premiumDurationMillis);
        
        PremiumTier tierDeserved;
        boolean deserveVeteranHonor; 
        
        if (premiumDays <= 30) {
            tierDeserved = PremiumTier.TIER_1;
            deserveVeteranHonor = false;
        } else if (premiumDays <= 60) {
            tierDeserved = PremiumTier.TIER_2;
            deserveVeteranHonor = true;
        } else {
            tierDeserved = PremiumTier.TIER_3;
            deserveVeteranHonor = true;
        }
        
        return setPerkForMember(member, guild, tierDeserved, deserveVeteranHonor, client, guildSettings, false);
    }

    private static boolean setPerkForMember(
        Member member, 
        Guild guild,
        PremiumTier tierDeserved, 
        boolean deserveVeteranHonor, 
        GatewayDiscordClient client, 
        @Nullable GuildSettings guildSettings,
        boolean forceSet
    ) {
        assert member != null;

        if (guildSettings == null) {
            guildSettings = Settings.forGuild(guild.getId().asLong());

            if (!validateGuildSettings(guildSettings, client)) {
                return false;
            }
        }

        assert guildSettings.getVipTier1RoleId() != null;
        assert guildSettings.getVipTier2RoleId() != null;
        assert guildSettings.getVipTier3RoleId() != null;
        assert guildSettings.getVipHonorRoleId() != null;
        assert guildSettings.getModChannelId() != null;
        
        PremiumTier tierCurrent = null;
        boolean hasVeteranHonor;
        
        Set<Long> memberRoleIds = member.getRoles().collectList().block()
            .stream()
            .map(role -> role.getId().asLong())
            .collect(Collectors.toSet());
        
        hasVeteranHonor = memberRoleIds.contains(guildSettings.getVipHonorRoleId());

        if (memberRoleIds.contains(guildSettings.getVipTier1RoleId())) {
            tierCurrent = PremiumTier.TIER_1;
        }
        if (memberRoleIds.contains(guildSettings.getVipTier2RoleId())) {
            tierCurrent = PremiumTier.TIER_2;
        }
        if (memberRoleIds.contains(guildSettings.getVipTier3RoleId())) {
            tierCurrent = PremiumTier.TIER_3;
        }

        boolean updatedTier = false;
        boolean awardedHonor = false;

        String reason = "VIP system perk update (by Jean-Pierre)";

        if (tierCurrent != tierDeserved
            && (tierCurrent == null || tierCurrent.getOrdinal() < tierDeserved.getOrdinal() || forceSet)) {
            
            if (tierDeserved == PremiumTier.TIER_1) {
                member.addRole(Snowflake.of(guildSettings.getVipTier1RoleId()), reason).block();
                
                member.removeRole(Snowflake.of(guildSettings.getVipTier2RoleId()), reason).block();
                member.removeRole(Snowflake.of(guildSettings.getVipTier3RoleId()), reason).block();
            } else if (tierDeserved == PremiumTier.TIER_2) {
                member.addRole(Snowflake.of(guildSettings.getVipTier2RoleId()), reason).block();
                
                member.removeRole(Snowflake.of(guildSettings.getVipTier1RoleId()), reason).block();
                member.removeRole(Snowflake.of(guildSettings.getVipTier3RoleId()), reason).block();
            } else if (tierDeserved == PremiumTier.TIER_3) {
                member.addRole(Snowflake.of(guildSettings.getVipTier3RoleId()), reason).block();

                member.removeRole(Snowflake.of(guildSettings.getVipTier1RoleId()), reason).block();
                member.removeRole(Snowflake.of(guildSettings.getVipTier2RoleId()), reason).block();
            }
            
            updatedTier = true;
        }

        if (!forceSet && deserveVeteranHonor && !hasVeteranHonor) {
            member.addRole(Snowflake.of(guildSettings.getVipHonorRoleId()), reason).block();
            awardedHonor = true;
        }

        if (updatedTier || awardedHonor) {
            postModUpdate(updatedTier, awardedHonor, tierCurrent, tierDeserved, member, guild, guildSettings, null);
            postMemberUpdate(updatedTier, awardedHonor, tierCurrent, tierDeserved, member, guild, guildSettings, null);
            
            return true;
        }
        
        return false;
    }

    private static void postModUpdate(
        boolean updatedTier, 
        boolean awardedHonor, 
        PremiumTier tierCurrent, 
        PremiumTier tierDeserved, 
        Member member, 
        Guild guild, 
        GuildSettings settings,
        @Nullable Long triggerUserId
    ) {
        assert settings.getModChannelId() != null;

        GuildChannel modChannel = guild.getChannelById(Snowflake.of(settings.getModChannelId())).block();

        if (modChannel instanceof GuildMessageChannel messageChannel) {
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .description(
                    "**VIP Member Perk Update**" +
                        "Member: <@" + member.getId().asLong() + ">\n" +
                        "Tier Now: " + tierDeserved + " (was " + tierCurrent + ")\n" +
                        "Awarded veteran honor: " + awardedHonor +
                        ((triggerUserId != null) ? "\nTriggered by: <@" + triggerUserId + ">" : "")
                )
                .color(Color.BISMARK)
                .build();

            messageChannel.createMessage(embed).subscribe();
        }
    }
    
    private static void postMemberUpdate(
        boolean updatedTier, 
        boolean awardedHonor, 
        PremiumTier tierCurrent, 
        PremiumTier tierDeserved, 
        Member member, 
        Guild guild,
        GuildSettings settings,
        @Nullable Long triggerUserId
    ) {
        assert settings.getBroadcastChannelId() != null;

        GuildChannel modChannel = guild.getChannelById(Snowflake.of(settings.getBroadcastChannelId())).block();

        if (modChannel instanceof GuildMessageChannel messageChannel) {
            String message = Branding.getPerkUpdateBroadcastMessage(tierDeserved, member.getId().asLong(), awardedHonor, settings, triggerUserId);
            
            if (message == null) {
                LOGGER.warn("Failed to create perk update broadcast message: " + tierDeserved + ", " + awardedHonor + ", " + member.getId().asLong());
            } else {
                messageChannel.createMessage(message).subscribe();
            }
        }
    }

    private static boolean validateGuildSettings(GuildSettings settings, GatewayDiscordClient client) {
        return settings.getBroadcastChannelId() != null
            && settings.getModChannelId() != null
            && settings.getVipHonorRoleId() != null
            && settings.getVipTier1RoleId() != null
            && settings.getVipTier2RoleId() != null
            && settings.getVipTier3RoleId() != null;
    }
}
