package org.example.server.commands;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;
import java.time.LocalDate;

public class Add extends Command {
    private final CollectionManager collectionManager;

    public Add(CollectionManager collectionManager) {
        super("add");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            Organization org = request.getOrganization();
            if (org == null) {
                return new Response(false, "Ошибка при передаче организации", null);
            }

            // Дату создания устанавливаем на сервере
            org.setCreationDate(LocalDate.now());

            // addToCollection теперь сам генерирует ID через БД и возвращает true/false
            boolean success = collectionManager.addToCollection(org);

            if (success) {
                // ID уже проставлен внутри addToCollection() после успешной вставки в БД
                return new Response(true, "Организация успешно добавлена с ID: " + org.getId(), null);
            } else {
                return new Response(false, "Ошибка при добавлении: объект не сохранён в базе данных", null);
            }
        } catch (Exception e) {
            return new Response(false, "Ошибка при добавлении: " + e.getMessage(), null);
        }
    }
}