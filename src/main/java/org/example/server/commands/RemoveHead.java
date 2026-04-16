package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;

import java.util.PriorityQueue;

public class RemoveHead extends Command{
    CollectionManager collectionManager;
    public RemoveHead(CollectionManager collectionManager){
        super("remove_head");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try{
            PriorityQueue<Organization> collection = collectionManager.getCollection();
            if (collection.isEmpty()){
                return new Response(true, "Коллекция пуста", null);
            }
            Organization head = collection.poll();
            return new Response(true, "Первый элемент коллекции:", head);
        } catch (Exception e) {
            return new Response(false, "Произошла ошибка: " + e.getMessage(), null);
        }
    }
}
