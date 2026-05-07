package org.example.server.manager;
import org.example.server.commands.Command;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandManager {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();
    private final List<String> commandHistory = new CopyOnWriteArrayList<>();
    private final Set<Path> executingScripts = ConcurrentHashMap.newKeySet();

    public void register(String name, Command command) {
        commands.put(name, command);
    }

    public Command getCommand(String name) {
        return commands.get(name);
    }

    public Map<String, Command> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public List<String> getCommandHistory() {
        return Collections.unmodifiableList(commandHistory);
    }

    public void addToCommandHistory(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > 6) {
            commandHistory.remove(0);
        }
    }

    public boolean isExecuteScript(Path path) {
        return executingScripts.contains(path);
    }

    public void addToExecuteScript(Path path) {
        executingScripts.add(path);
    }

    public void removeFromExecuteScript(Path path) {
        executingScripts.remove(path);
    }
}