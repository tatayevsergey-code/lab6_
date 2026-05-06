package org.example.server.commands;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.util.PasswordUtil;
import org.example.server.manager.CollectionManager;
import org.example.server.manager.DatabaseManager;
import java.sql.SQLException;

public class RegisterCommand extends Command { // Замените Command на ваш интерфейс команд
    CollectionManager collectionManager;
    public RegisterCommand(CollectionManager collectionManager) {
        super("register");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String login = request.getUser();
        String password = request.getPassword();
        String hash = PasswordUtil.hashSHA1(password);
        try {
            return collectionManager.getDbManager().registerUser(login, hash)
                    ? new Response(true,"Регистрация успешна", null)
                    : new Response(false,"Пользователь с таким логином уже существует", null);
        } catch (SQLException e) {
            return new Response(false,"Ошибка БД: " + e.getMessage(), null);
        }
    }
}