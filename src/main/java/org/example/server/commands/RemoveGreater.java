package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;

import java.util.Collection;
import java.util.Iterator;

public class RemoveGreater extends Command{
    CollectionManager collectionManager;
    public RemoveGreater(CollectionManager collectionManager){
        super("remove_greater");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            String argument = request.getArgument().toString();

            long idMax;
            try {
                idMax = Long.parseLong(argument.trim());
            } catch (NumberFormatException e) {
                return new Response(false, "id должен быть целым числом", null);
            }
            Collection<Organization> collection = collectionManager.getCollection();
            if (collection.isEmpty()) {
                return new Response(true, "Коллекция пуста", null);
            }

            Iterator<Organization> iterator = collection.iterator();
            int removedCount = 0;
            while (iterator.hasNext()) {
                Organization organization = iterator.next();
                if (organization.getId() > idMax) {
                    iterator.remove();
                    removedCount++;
                }
            }
            if (removedCount == 0) {
                return new Response(true, "Нет элементов, которые превышают id = " + idMax, null);
            } else {
                return new Response(true, "Элементы, превышающие id = " + idMax + " удалены", null);
            }
        } catch (Exception e) {
            return new Response(false, "Произошла ошибка: " + e.getMessage(), null);
        }
    }
}
