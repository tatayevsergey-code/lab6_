package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrintFieldDescendingType extends Command{
    CollectionManager collectionManager;
    public PrintFieldDescendingType(CollectionManager collectionManager){
        super("print_field_descending_type");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            List<Organization> list = new ArrayList<>(collectionManager.getCollection());
            if (list.isEmpty()){
                return new Response(true, "Коллекция пуста", null);
            }

            StringBuilder result = new StringBuilder();
            for (Organization organization : list) {
                result.append(organization.getType()).append("\n");
            }
            return new Response(true, "Поле type в порядке убывания id", result);
        } catch (Exception e){
            return new Response(false, "Произошла ошибка: " + e.getMessage(), null);
        }
    }
}
