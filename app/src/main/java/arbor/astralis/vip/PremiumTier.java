package arbor.astralis.vip;

public enum PremiumTier {
    
    TIER_1(0),
    TIER_2(1),
    TIER_3(2)
    
    ;
    
    private final int ordinal;
    
    PremiumTier(int ordinal) {
        this.ordinal = ordinal;
    }
    
    public int getOrdinal() {
        return ordinal;
    }
}
