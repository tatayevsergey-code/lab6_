package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;

public abstract class Command {
    private final String name;

    public Command(String name){
        this.name = name;
    }
    public abstract Response execute(Request request);

    public String getName() {
        return name;
    }
}
