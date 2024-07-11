package arbor.astralis.vip;

import discord4j.core.spec.EmbedCreateSpec;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Branding {

    public static final String BOT_NAME = "Jean-Pierre";
    public static final String MAINTAINER_NAME = "Haru";

    public static String getResetColorRolesSuccessMessage(List<Long> roleIdsRemoved) {
        if (roleIdsRemoved == null || roleIdsRemoved.isEmpty()) {
            return "Howdy partner! As you do not currently have a VIP color role, no roles were removed. :rose:";
        } else {
            return "Done deal partner! I've removed the following role(s) from you: " +
                roleIdsRemoved.stream()
                    .map(id -> "<@&" + id + ">")
                    .collect(Collectors.joining(", "));
        }
    }
    
    public static String getAdminOnlyCommandMessage() {
        return "Easy there, partner! That one's off-limits.";
    }

    private static String takeRandom(String ... strings) {
        int length = strings.length;
        return strings[(int) (Math.random() * length)];
    }

    public static String getUnexpectedErrorMessage() {
        return "Oops! An unexpected error occurred, please contact " + Branding.MAINTAINER_NAME;
    }

    public static String getPerkUpdateBroadcastMessage(
        PremiumTier tierDeserved, 
        long userId, 
        boolean awardedHonor, 
        GuildSettings settings,
        @Nullable Long triggerUserId
    ) {
        String message = null;
        
        if (triggerUserId == null) {
            if (tierDeserved == PremiumTier.TIER_1) {
                message = takeRandom(
                    "Welcome to the VIP club, <@" + userId + ">! It's a pleasure to have you with us~\n" +
                        "Check out the VIP specials channel for your rewards :rose:"
                );
            } else if (tierDeserved == PremiumTier.TIER_2) {
                message = takeRandom(
                    "Moving on up, <@" + userId + ">! Thank you for your continued support of this community.\n" +
                        "As a second-month booster, you have earned the VIP Platinum pass.\n\nCheck out the VIP specials channel for your rewards!" 
                );
            } else if (tierDeserved == PremiumTier.TIER_3) {
                message = takeRandom(
                    "Salutations, <@" + userId + ">! Thank you for another month of steadfast support for this community :champagne_glass:\n" +
                        "You have earned the VIP Deluxe pass.\n\nCheck out the VIP specials channel for your new rewards!"
                );
            }
        } else {
            if (tierDeserved == PremiumTier.TIER_1) {
                return takeRandom(
                    "Hello <@" + userId + ">, this is to let you know that <@" + triggerUserId + "> has set your VIP perks to VIP - Gold pass (tier 1) :rose:"
                );
            } else if (tierDeserved == PremiumTier.TIER_2) {
                return takeRandom(
                    "Greetings <@" + userId + ">, this is to let you know that <@" + triggerUserId + "> has set your VIP perks to VIP - Platinum pass (tier 2) :rose:"
                );
            } else if (tierDeserved == PremiumTier.TIER_3) {
                return takeRandom(
                    "Good day <@" + userId + ">, this is to let you know that <@" + triggerUserId + "> has set your VIP perks to VIP - Deluxe pass (tier 3) :rose:"
                );
            }
        }

        if (awardedHonor) {
            message += "\n\nIn addition, for your uninterrupted support, you have been awarded the permanent honor role: VIP Club Veteran!";
        }
        
        return message;
    }
}
