package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;

import java.util.Collection;

public class Show extends Command{
    CollectionManager collectionManager;

    public Show(CollectionManager collectionManager){
        super("add");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            Collection<Organization> collection = collectionManager.getCollection();

            if (collection.isEmpty()) {
                return new Response(true, "Коллекция пуста", null);
            }

            StringBuilder result = new StringBuilder();
            for (Organization org : collection) {
                result.append(org.toString()).append("\n");
            }

            int size = collectionManager.size();

            return new Response(true, "В коллекции " + size + " элемент/ов, отсортированный по годовому обороту", result);
        } catch (Exception e){
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}
