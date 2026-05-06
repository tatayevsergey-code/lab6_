package org.example.server.commands;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;
import org.example.server.manager.CollectionManager;

public class Update extends Command {
    private final CollectionManager collectionManager;

    public Update(CollectionManager collectionManager) {
        super("update");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            Object arg = request.getArgument();
            if (arg == null) {
                return new Response(false, "Укажите id организации", null);
            }

            long id;
            try {
                id = Long.parseLong(arg.toString().trim());
            } catch (NumberFormatException e) {
                return new Response(false, "id должен быть целым числом", null);
            }

            Organization oldOrg = collectionManager.getById(id);
            Organization newOrg = request.getOrganization();
            if (oldOrg == null) {
                return new Response(false, "Организация с id=" + id + " не найдена", null);
            }
            else {
                if(oldOrg.getUsername().equals(request.getUser())) {
                    if (newOrg == null) {
                        return new Response(false, "Нет данных для обновления", null);
                    }
                } else {
                    return new Response(false, "Нет прав для обновления информации по данной организации", null);
                }
            }



            // явно сохраняем оригинальный ID и дату создания
            newOrg.setId(oldOrg.getId());
            newOrg.setCreationDate(oldOrg.getCreationDate());

            // Делегируем менеджеру. Он сам обновит БД и безопасно заменит объект в памяти.
            boolean success = collectionManager.updateOrganization(newOrg);

            if (success) {
                return new Response(true, "Организация с id=" + id + " успешно обновлена", null);
            } else {
                return new Response(false, "Ошибка при обновлении в базе данных", null);
            }
        } catch (Exception e) {
            return new Response(false, "Ошибка: " + e.getMessage(), null);
        }
    }
}