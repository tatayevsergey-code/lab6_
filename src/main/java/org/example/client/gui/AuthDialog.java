package org.example.client.gui;

import org.example.client.network.ClientNetworkManager;
import org.example.common.Request;
import org.example.common.Response;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.SocketException;

public class AuthDialog extends JDialog {
    private final ClientNetworkManager networkManager;
    private final JTextField loginField;
    private final JPasswordField passwordField;
    private boolean success = false;
    private String username;

    public AuthDialog(ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
        setTitle("Авторизация / Регистрация");
        setModal(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        loginField = new JTextField(15);
        passwordField = new JPasswordField(15);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Логин:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(loginField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("Пароль:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(passwordField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton loginBtn = new JButton("Войти");
        JButton registerBtn = new JButton("Регистрация");

        loginBtn.addActionListener(e -> authenticate("login"));
        registerBtn.addActionListener(e -> authenticate("register"));

        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private void authenticate(String command) {
        String user = loginField.getText().trim();
        String pass = new String(passwordField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните оба поля", "Ошибка ввода", JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginField.setEnabled(false);
        passwordField.setEnabled(false);

        try {
            Request request = new Request(command, null, null);
            request.setUser(user);
            request.setPassword(pass);

            System.out.println("[Auth] Отправляем запрос: " + command + " " + user);
            Response response = networkManager.sendRequest(request);

            if (response != null && response.isSuccess()) {
                this.success = true;
                this.username = user;
                System.out.println("✅ " + response.getMessage());
                dispose();
            } else {
                String msg = response != null ? response.getMessage() : "Сервер не вернул ответ";
                JOptionPane.showMessageDialog(this, msg, "Ошибка сервера", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.err.println("[Auth] Ошибка: " + e.getMessage());
            e.printStackTrace(); // Вывод полного стека в консоль IDE

            String errorMsg = e.getMessage() != null && e.getMessage().contains("closed")
                    ? "Соединение разорвано. Убедитесь, что сервер запущен, и перезапустите клиент."
                    : "Ошибка соединения: " + e.getMessage();

            JOptionPane.showMessageDialog(this, errorMsg, "Ошибка", JOptionPane.ERROR_MESSAGE);
        } finally {
            loginField.setEnabled(true);
            passwordField.setEnabled(true);
            loginField.requestFocus();
        }
    }

    public boolean isSuccess() { return success; }
    public String getUsername() { return username; }
}