package org.example.server;

import org.example.server.commands.*;
import org.example.server.handlers.RequestHandler;
import org.example.server.manager.CollectionManager;
import org.example.server.manager.CommandManager;
import org.example.server.manager.DatabaseManager;
import org.example.server.network.ServerNetworkManager;
import java.sql.SQLException;

public class ServerApp {
    private static final int port = 12345;

    public static void main(String[] args) {
        String dbUrl = /*null;*/ System.getenv("DB_URL");
        String dbUser = /*null;*/ System.getenv("DB_USER");
        String dbPass = /*null;*/ System.getenv("DB_PASS");

        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:postgresql://localhost:5432/java_lab6_db";
            System.out.println("DB_URL не задана, используется localhost:5432/java_lab6_db");
        }
        if (dbUser == null) dbUser = "postgres";
        if (dbPass == null) dbPass = "12345678";

        DatabaseManager dbManager = new DatabaseManager(dbUrl, dbUser, dbPass);
        try {
            dbManager.connect();
        } catch (SQLException e) {
            System.err.println("Не удалось подключиться к PostgreSQL: " + e.getMessage());
            System.exit(1);
        }

        CollectionManager collectionManager = new CollectionManager(dbManager);

        CommandManager commandManager = new CommandManager(){{
            register("help", new Help());
            register("add", new Add(collectionManager));
            register("show", new Show(collectionManager));
            register("info", new Info(collectionManager));
            register("history", new History(this));
            register("clear", new Clear(collectionManager));
            register("print_descending", new PrintDescending(collectionManager));
            register("print_field_descending_type", new PrintFieldDescendingType(collectionManager));
            register("remove_head", new RemoveHead(collectionManager));
            register("remove_greater", new RemoveGreater(collectionManager));
            register("remove_by_id", new RemoveById(collectionManager));
            register("remove_any_by_official_address", new RemoveAnyByOfficialAddress(collectionManager));
            register("update", new Update(collectionManager));
        }};

        RequestHandler handler = new RequestHandler(commandManager);

        // Shutdown hook: логируем сохранение и закрываем соединение
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            collectionManager.saveCollection();
            dbManager.close();
            System.out.println("Сервер остановлен. Соединение с БД закрыто.");
        }));

        try {
            ServerNetworkManager server = new ServerNetworkManager(port);
            server.start(handler);
        } catch (Exception e) {
            System.out.println("Ошибка запуска сервера: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}