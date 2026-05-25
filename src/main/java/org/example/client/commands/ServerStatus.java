package org.example.client.commands;

import org.example.client.network.ClientNetworkManager;
import org.example.common.Request;
import org.example.common.Response;

import java.io.IOException;

public class ServerStatus extends Command{
    ClientNetworkManager networkManager;
    public ServerStatus(ClientNetworkManager networkManager){
        this.networkManager = networkManager;
    }

    @Override
    public Response execute(String argument) {
        try{
            Request request = new Request("server_status", null, null);
            Response response = networkManager.sendRequest(request);
            System.out.println(response.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
        return null;
    }
}
