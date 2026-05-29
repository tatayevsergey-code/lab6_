package org.example.client.util;

import org.example.client.commands.*;
import org.example.client.gui.AuthDialog;
import org.example.client.gui.CollectionTableFrame;
import org.example.client.network.ClientNetworkManager;
import org.example.common.Request;
import org.example.common.Response;
import org.example.common.models.Organization;

import javax.swing.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Runner {
    private final Scanner scanner;
    private final ClientNetworkManager networkManager;
    private final Map<String, Command> commands = new HashMap<>();

    public Runner(Scanner scanner, ClientNetworkManager networkManager) {
        this.scanner = scanner;
        this.networkManager = networkManager;

        commands.put("help", new Help(networkManager));
        commands.put("add", new Add(scanner, networkManager));
        commands.put("show", new Show(networkManager));
        commands.put("info", new Info(networkManager));
        commands.put("history", new History(networkManager));
        commands.put("clear", new Clear(networkManager));
        commands.put("print_descending", new PrintDescending(networkManager));
        commands.put("print_field_descending_type", new PrintFieldDescendingType(networkManager));
        commands.put("remove_head", new RemoveHead(networkManager));
        commands.put("remove_greater", new RemoveGreater(networkManager));
        commands.put("remove_by_id", new RemoveById(networkManager));
        commands.put("remove_any_by_official_address", new RemoveAnyByOfficialAddress(networkManager));
        commands.put("update", new Update(scanner, networkManager));
        commands.put("execute_script", new ExecuteScript(networkManager, scanner));
        commands.put("exit", new Exit());

        commands.put("register", new Register(networkManager));
        commands.put("login", new Login(networkManager));
        //commands.put("logout", new Logout());

        commands.put("server_status", new ServerStatus(networkManager));
    }

//        public void interactiveMode() {
//
//            boolean authorized = false;
//            while (!authorized) {
//                System.out.print("Введите команду авторизации (login <логин> <пароль> / register <логин> <пароль>): ");
//                String input = scanner.nextLine().trim();
//                if (input.isEmpty()) continue;
//
//                String[] parts = input.split(" ", 3);
//                String commandName = parts[0];
//                if(parts.length < 3) continue;//throw new IllegalArgumentException("Неверный формат ввода");
//                String username = parts[1], password = parts[2];
//                Command command = commands.get(commandName);
//                if (command == null) {
//                    System.out.println("Неизвестная команда, введите еще раз");
//                    continue;
//                }
//                Response resp = command.execute(/*argument*/username + " " + password);
//
//                if (resp.isSuccess() && (commandName.equals("login") || commandName.equals("register"))) {
//                    authorized = true;
//                }
//            }
//
//            System.out.println("Введите команду");
//            while (true) {
//                try {
//                    System.out.println("> ");
//                    String line = scanner.nextLine().trim();
//                    if (line.isEmpty()) {
//                        continue;
//                    }
//
//                    String[] parts = line.split(" ", 2);
//                    String commandName = parts[0];
//                    String argument = parts.length > 1 ? parts[1] : null;
//                    Command command = commands.get(commandName);
//                    if (command == null) {
//                        System.out.println("Неизвестная команда, введите еще раз");
//                        continue;
//                    }
//                    command.execute(argument);
//                } catch (Exception e){
//                    System.err.println("Ошибка: " + e.getMessage());
//                }
//            }
//        }

        public void interactiveMode() {
            // 1. Инициализируем Swing
            SwingUtilities.invokeLater(() -> {
                AuthDialog authDialog = new AuthDialog(networkManager);
                authDialog.setVisible(true); // Блокирует поток EDT до закрытия окна

                if (authDialog.isSuccess()) {
                    String username = authDialog.getUsername();
                    System.out.println("✅ Авторизация успешна. Добро пожаловать, " + username + "!");
                    System.out.println("Введите команду (или 'help' для справки):");
                    //startCommandLoop();
                    // 🔹 Загружаем таблицу в фоне, чтобы не морозить UI
                    new Thread(() -> {
                        try {
                            Request showReq = new Request("show", null, null);
                            showReq.setUser(username);
                            Response showResp = networkManager.sendRequest(showReq);

                            if (showResp != null && showResp.isSuccess() && showResp.getData() instanceof Collection) {
                                Collection<Organization> data = (Collection<Organization>) showResp.getData();
                                // Показываем таблицу строго в EDT
                                SwingUtilities.invokeLater(() -> new CollectionTableFrame(data, username,networkManager).setVisible(true));
                            }
                        } catch (Exception e) {
                            System.err.println("⚠️ Не удалось загрузить коллекцию: " + e.getMessage());
                        }
                    }).start();

                    System.out.println("Введите команду (или 'help' для справки):");
                    // Консольный цикл запускаем в отдельном потоке, чтобы он не блокировал Swing EDT
                    new Thread(this::startCommandLoop, "ConsoleInputThread").start();
                } else {
                    System.out.println("❌ Авторизация отменена. Завершение работы клиента.");
                    System.exit(0);
                }
            });

            // Ждём завершения потока EDT, чтобы клиент не закрылся сразу
            try {
                SwingUtilities.invokeAndWait(() -> {
                });
            } catch (Exception ignored) {
            }
        }

        private void startCommandLoop() {
            while (true) {
                try {
                    System.out.print("> ");
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty()) continue;

                    String[] parts = line.split("\\s+", 2);
                    String commandName = parts[0];
                    String argument = parts.length > 1 ? parts[1] : null;

                    Command command = commands.get(commandName);
                    if (command == null) {
                        System.out.println("⚠️ Неизвестная команда. Введите 'help' для справки.");
                        continue;
                    }
                    command.execute(argument);
                } catch (Exception e) {
                    System.err.println("❌ Ошибка: " + e.getMessage());
                }
            }
        }
    }