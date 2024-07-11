package arbor.astralis.vip;

import java.util.Set;
import java.util.stream.Collectors;

public final class Branding {

    public static final String BOT_NAME = "Jean-Pierre";

    public static String getResetColorRolesSuccessMessage(Set<Long> roleIdsRemoved) {
        if (roleIdsRemoved == null || roleIdsRemoved.isEmpty()) {
            return "Howdy partner! As you do not currently have a VIP color role, no roles were removed. :rose:";
        } else {
            return "Done deal partner! The following roles have been removed: " +
                roleIdsRemoved.stream()
                    .map(id -> "<@&" + id + ">")
                    .collect(Collectors.joining(", "));
        }
    }

    private static String takeRandom(String ... strings) {
        int length = strings.length;
        return strings[(int) (Math.random() * length)];
    }
}
