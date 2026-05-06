package org.example.client.commands;

import org.example.common.Response;

public class Exit extends Command{

    @Override
    public Response execute(String argument) {
        System.out.println("Работа завершена");
        System.exit(0);
        return null;
    }
}
