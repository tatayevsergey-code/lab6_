package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.server.manager.CollectionManager;

public class Clear extends Command{
    CollectionManager collectionManager;
    public Clear(CollectionManager collectionManager){
        super("clear");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
         try {
             collectionManager.clearCollection();
             return new Response(true, "Коллекция очищена", null);
         } catch (Exception e) {
             return new Response(false, "Произошла ошибка: " + e.getMessage(), null);
         }
    }
}
