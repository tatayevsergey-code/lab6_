package org.example.client.commands;

import org.example.client.network.ClientNetworkManager;
import org.example.common.Request;
import org.example.common.Response;

import java.io.IOException;

public class Login extends Command {
    ClientNetworkManager networkManager;
    public Login(ClientNetworkManager networkManager){
        this.networkManager = networkManager;
    }

    @Override
    public Response execute(String argument) {
        try{
            String[] arguments = argument.split(" ");
            if(arguments.length != 2) throw new IllegalArgumentException("Неверное количество аргументов");
            Request request = new Request("login", null, null);
            request.setUser(arguments[0]);
            request.setPassword(arguments[1]);
            Response response = networkManager.sendRequest(request);
            System.out.println(response.getMessage());
            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Ошибка: " + e.getMessage());
            return null;
        }
    }
}
