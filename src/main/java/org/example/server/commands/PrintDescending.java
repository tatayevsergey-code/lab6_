package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrintDescending extends Command{
    CollectionManager collectionManager;
    public PrintDescending(CollectionManager collectionManager){
        super("print_descending");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        List<Organization> list = new ArrayList<>(collectionManager.getCollection());
        try{
            if (list.isEmpty()){
                return new Response(true, "Коллекция пуста", null);
            }
            list.sort(Collections.reverseOrder());
            StringBuilder result = new StringBuilder();
            for (Organization organization : list) {
                result.append(organization).append("\n");
            }
            return new Response(true, "Элементы коллекции в порядке убывания", result);
        } catch (Exception e) {
            return new Response(false, "Произошла ошибка: " + e.getMessage(), null);
        }

    }
}
