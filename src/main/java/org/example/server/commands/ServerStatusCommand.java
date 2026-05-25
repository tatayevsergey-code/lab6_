package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;
import org.example.server.network.ServerNetworkManager;

import java.util.Collection;

public class ServerStatusCommand extends Command{
    CollectionManager collectionManager;
    ServerNetworkManager server;

    public ServerStatusCommand(CollectionManager collectionManager, ServerNetworkManager server){
        super("server_status");
        this.collectionManager = collectionManager;
        this.server = server;
    }

    @Override
    public Response execute(Request request) {
        try {
            Collection<Organization> collection = collectionManager.getCollection();

            StringBuilder result = new StringBuilder();
            result.append("Количество клиентов: " + collectionManager.getClientCount() + "\n");
            result.append("Количество активных потоков: " + server.getTotalActiveThreads() + "\n");
            result.append("Количество запросов в работе: " + collectionManager.getRequestInProgressCount() + "\n");
            result.append("Количество выполненных запросов: " + collectionManager.getRequestPerformedCount() + "\n");

            return new Response(true, result.toString(),null);
        } catch (Exception e){
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}

