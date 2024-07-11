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
        
        List<Member> vipObserved = new ArrayList<>();
        List<Member> vipUpdated = new ArrayList<>();
        
        for (Member member : members) {
            if (member == null) {
                continue;
            }

            Optional<Instant> premiumTime = member.getPremiumTime();
            if (premiumTime.isPresent()) {
                vipObserved.add(member);
                
                if (refreshPerksForMember(member, premiumTime.get(), guild, client, settings)) {
                    vipUpdated.add(member);
                }
            }
        }

        assert settings.getModChannelId() != null;
        
        GuildChannel modChannel = guild.getChannelById(Snowflake.of(settings.getModChannelId())).block();
        
        if (modChannel instanceof GuildMessageChannel messageChannel) {
            var vipObservedNames = new StringBuilder();
            for (Member member : vipObserved) {
                String vipDays = "n/a";

                Optional<Instant> premiumTime = member.getPremiumTime();
                if (premiumTime.isPresent()) {
                    long premiumStart = premiumTime.get().toEpochMilli();
                    long now = Instant.now().toEpochMilli();
                    long durationMs = now - premiumStart;
                    long durationDays = TimeUnit.MILLISECONDS.toDays(durationMs);
                    
                    vipDays = durationDays + " day(s)";
                }
                
                vipObservedNames.append("<@").append(member.getId().asLong()).append(">")
                    .append(" - vip time: ").append(vipDays).append("\n");
            }

            var vipUpdatedNames = new StringBuilder();
            for (Member member : vipUpdated) {
                vipUpdatedNames.append("<@").append(member).append(">").append("\n");
            }
            
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .description(
                    "**Completed VIP Perk Refresh**\n" +
                        "\n" +
                        "VIPs observed: " + vipObserved.size() + "\n" + vipObservedNames + "\n\n" +
                        "VIPs updated: " + vipUpdated.size() + "\n" + vipUpdatedNames
                )
                .color(Color.BISMARK)
                .build();
            
            messageChannel.createMessage(embed).subscribe();
        }
    }

    public static boolean refreshPerksForMember(
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

        PremiumTier tierCurrent = null;
        PremiumTier tierDeserved;
        boolean deserveVeteranHonor;
        
        Set<Long> memberRoleIds = member.getRoles().collectList().block()
            .stream()
            .map(role -> role.getId().asLong())
            .collect(Collectors.toSet());

        if (memberRoleIds.contains(guildSettings.getVipTier1RoleId())) {
            tierCurrent = PremiumTier.TIER_1;
        }
        if (memberRoleIds.contains(guildSettings.getVipTier2RoleId())) {
            tierCurrent = PremiumTier.TIER_2;
        }
        if (memberRoleIds.contains(guildSettings.getVipTier3RoleId())) {
            tierCurrent = PremiumTier.TIER_3;
        }
        
        if (premiumDays <= 30) {
            tierDeserved = getThisOrNextTier(PremiumTier.TIER_1, tierCurrent);
            deserveVeteranHonor = false;
        } else if (premiumDays <= 60) {
            tierDeserved = getThisOrNextTier(PremiumTier.TIER_2, tierCurrent);
            deserveVeteranHonor = true;
        } else {
            tierDeserved = getThisOrNextTier(PremiumTier.TIER_3, tierCurrent);
            deserveVeteranHonor = true;
        }
        
        return setPerkForMember(member, guild, tierDeserved, deserveVeteranHonor, client, guildSettings, null, false);
    }

    private static PremiumTier getThisOrNextTier(PremiumTier defaultNextTier, PremiumTier currentTier) {
        if (currentTier == null) {
            return defaultNextTier;
        }
        
        if (defaultNextTier == PremiumTier.TIER_3) {
            return defaultNextTier;
        }
        
        if (defaultNextTier.getOrdinal() <= currentTier.getOrdinal()) {
            int nextOrdinal = Math.min(PremiumTier.TIER_3.getOrdinal(), currentTier.getOrdinal() + 1);
            Optional<PremiumTier> premiumTier = PremiumTier.fromOrdinal(nextOrdinal);
            return premiumTier.orElse(defaultNextTier);
        }
        
        return defaultNextTier;
    }

    public static boolean setPerkForMember(
        Member member, 
        Guild guild,
        PremiumTier tierDeserved, 
        boolean deserveVeteranHonor, 
        GatewayDiscordClient client, 
        @Nullable GuildSettings guildSettings,
        @Nullable Long triggerUserId,
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
            postModUpdate(updatedTier, awardedHonor, tierCurrent, tierDeserved, member, guild, guildSettings, triggerUserId);
            postMemberUpdate(updatedTier, awardedHonor, tierCurrent, tierDeserved, member, guild, guildSettings, triggerUserId);
            
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
                    "**VIP Member Perk Update**\n\n" +
                        "**Member:** <@" + member.getId().asLong() + ">\n" +
                        "**Tier Now:** " + tierDeserved + " (was " + tierCurrent + ")\n" +
                        "**Awarded Honor:** " + awardedHonor +
                        ((triggerUserId != null) ? "\n**Triggered by:** <@" + triggerUserId + ">" : "")
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
