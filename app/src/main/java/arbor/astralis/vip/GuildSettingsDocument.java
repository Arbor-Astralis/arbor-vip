package arbor.astralis.vip;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GuildSettingsDocument {
    
    private List<Long> vipColorRoleIds = new ArrayList<>();

    private @Nullable Long vipTier1RoleId = null;
    private @Nullable Long vipTier2RoleId = null;
    private @Nullable Long vipTier3RoleId = null;
    private @Nullable Long vipHonorRoleId = null;
    
    private @Nullable Long broadcastChannelId = null;
    private @Nullable Long modChannelId = null;
    
    public List<Long> getVipColorRoleIds() {
        return vipColorRoleIds;
    }

    public void setVipColorRoleIds(List<Long> vipColorRoleIds) {
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

    @Nullable
    public Long getModChannelId() {
        return modChannelId;
    }

    public void setModChannelId(@Nullable Long modChannelId) {
        this.modChannelId = modChannelId;
    }
}
