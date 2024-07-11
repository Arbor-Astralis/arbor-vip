package arbor.astralis.vip;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public final class GuildSettings {
    
    public static final int MONTHS_ACTIVE_FOR_VIP_HONOR_ROLE = 2; // second month and onwards
    
    private final long guildId;
    
    private Set<Long> vipColorRoleIds = new HashSet<>();
    
    private @Nullable Long vipTier1RoleId = null;
    private @Nullable Long vipTier2RoleId = null;
    private @Nullable Long vipTier3RoleId = null;
    private @Nullable Long vipHonorRoleId = null;

    private @Nullable Long broadcastChannelId = null;
    
    public GuildSettings(long guildId) {
        this.guildId = guildId;
    }

    public long getGuildId() {
        return guildId;
    }

    public Set<Long> getVipColorRoleIds() {
        return vipColorRoleIds;
    }

    public void setVipColorRoleIds(Set<Long> vipColorRoleIds) {
        this.vipColorRoleIds = vipColorRoleIds;
    }

    @Nullable
    public Long getVipTier1RoleId() {
        return vipTier1RoleId;
    }

    public void setVipTier1RoleId(@Nullable Long vipTier1RoleId) {
        this.vipTier1RoleId = vipTier1RoleId;
    }

    @Nullable
    public Long getVipTier2RoleId() {
        return vipTier2RoleId;
    }

    public void setVipTier2RoleId(@Nullable Long vipTier2RoleId) {
        this.vipTier2RoleId = vipTier2RoleId;
    }

    @Nullable
    public Long getVipTier3RoleId() {
        return vipTier3RoleId;
    }

    public void setVipTier3RoleId(@Nullable Long vipTier3RoleId) {
        this.vipTier3RoleId = vipTier3RoleId;
    }

    @Nullable
    public Long getVipHonorRoleId() {
        return vipHonorRoleId;
    }

    public void setVipHonorRoleId(@Nullable Long vipHonorRoleId) {
        this.vipHonorRoleId = vipHonorRoleId;
    }

    @Nullable
    public Long getBroadcastChannelId() {
        return broadcastChannelId;
    }

    public void setBroadcastChannelId(@Nullable Long broadcastChannelId) {
        this.broadcastChannelId = broadcastChannelId;
    }
}