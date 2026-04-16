package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Address;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;

import java.util.Collection;
import java.util.Iterator;

public class RemoveAnyByOfficialAddress extends Command{
    CollectionManager collectionManager;
    public RemoveAnyByOfficialAddress(CollectionManager collectionManager){
        super("remove_any_by_official_address");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            Object arg = request.getArgument();
            String street = null;

            if (arg != null) {
                street = (String) arg;
            }

            if (street == null || street.trim().isEmpty()) {
                return new Response(false, "Укажите название улицы", null);
            }

            street = street.trim();
            Collection<Organization> collection = collectionManager.getCollection();
            boolean found = false;

            if (collection.isEmpty()){
                return new Response(true, "Коллекция пуста", null);
            }

            Iterator<Organization> iterator = collection.iterator();
            while (iterator.hasNext()) {
                Organization org = iterator.next();
                Address addr = org.getOfficialAddress();
                if (addr != null && street.equals(addr.getStreet())) {
                    iterator.remove();
                    found = true;
                    break;
                }
            }
            if (!found){
                return new Response(true, "Элемент с улицей: " + street + " не найден", null);
            } else {
                return new Response(true, "Элемент с адресом: " + street + " удалён", null);
            }
        } catch (Exception e) {
            return new Response(false, "Произошла ошибка: " + e.getMessage(), null);
        }
    }
}
