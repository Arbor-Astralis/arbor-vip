package arbor.astralis.vip.commands;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ApplicationCommands {
    
    private static final Map<String, ApplicationCommand> COMMANDS_BY_NAME = new LinkedHashMap<>();
    
    static {
        registerCommand(new HelpCommand());
        registerCommand(new VipSystemAdminCommands());
    }
    
    private ApplicationCommands() {
        // Utility class, no instantiation
    }
    
    private static void registerCommand(ApplicationCommand command) {
        String commandName = command.getName();
        COMMANDS_BY_NAME.put(commandName, command);
    }
    
    public static Collection<ApplicationCommand> getAll() {
        return COMMANDS_BY_NAME.values();
    }

    public static Optional<ApplicationCommand> forName(String name) {
        return Optional.ofNullable(COMMANDS_BY_NAME.get(name));
    }
}
