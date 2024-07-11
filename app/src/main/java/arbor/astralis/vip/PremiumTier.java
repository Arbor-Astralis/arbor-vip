package arbor.astralis.vip;

import java.util.Optional;

public enum PremiumTier {
    
    TIER_1(1),
    TIER_2(2),
    TIER_3(3)
    ;
    
    private final int ordinal;
    
    PremiumTier(int ordinal) {
        this.ordinal = ordinal;
    }
    
    public int getOrdinal() {
        return ordinal;
    }
    
    public static Optional<PremiumTier> fromOrdinal(int value) {
        for (PremiumTier tier : values()) {
            if (tier.ordinal == value) {
                return Optional.of(tier);
            }
        }
        
        return Optional.empty();
    }
}
