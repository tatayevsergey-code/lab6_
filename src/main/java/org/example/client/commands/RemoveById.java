package org.example.client.commands;

import org.example.client.network.ClientNetworkManager;
import org.example.common.Request;
import org.example.common.Response;

import java.io.IOException;

public class RemoveById extends Command{
    ClientNetworkManager clientNetworkManager;
    public RemoveById(ClientNetworkManager clientNetworkManager){
        this.clientNetworkManager = clientNetworkManager;
    }

    @Override
    public Response execute(String argument) {
        try {
            if (argument == null || argument.trim().isEmpty()) {
                System.out.println("Неверный формат команды, введите id");
                return null;
            }
            String idArg = argument.trim();

            Request request = new Request("remove_by_id", idArg, null);
            Response response = clientNetworkManager.sendRequest(request);
            System.out.println(response.getMessage());
        } catch (IOException | ClassNotFoundException e){
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
        return null;
    }
}
