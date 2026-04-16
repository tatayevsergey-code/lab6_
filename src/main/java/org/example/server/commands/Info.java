package org.example.server.commands;

import org.example.common.Request;
import org.example.common.Response;
import org.example.server.manager.CollectionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Info extends Command{
    CollectionManager collectionManager;

    public Info(CollectionManager collectionManager){
        super("info");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try{
            LocalDateTime time = collectionManager.getTime();

            String formattedTime = time.format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
            );

            String info = "Тип коллекции: " + collectionManager.getCollection().getClass().getSimpleName() + "\n" +
            "Дата инициализации: " + formattedTime + "\n" + "Колличество элементов: " + collectionManager.size();
            return new Response(true, info, null);

        } catch (Exception e) {
            return new Response(false, "Произошла ошибка при выводе информации: " + e.getMessage(), null);
        }
    }
}
