package org.example.server.commands;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.util.PasswordUtil;
import org.example.server.manager.CollectionManager;
import org.example.server.manager.DatabaseManager;
import java.sql.SQLException;

public class LoginCommand extends Command {
    CollectionManager collectionManager;
    public LoginCommand(CollectionManager collectionManager) {
        super("login");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String login = request.getUser();
        String password = request.getPassword();
        String hash = PasswordUtil.hashSHA1(password);
        try {
            return collectionManager.getDbManager().authenticateUser(login, hash)
                    ? new Response(true,"Авторизация успешна", null)
                    : new Response(false,"Неверный логин или пароль", null);
        } catch (SQLException e) {
            return new Response(false,"Ошибка БД: " + e.getMessage(), null);
        }
    }
}
