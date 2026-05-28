package org.example.client.network;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.util.SerializationUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Менеджер сетевого взаимодействия для клиента.
 * Отвечает за подключение к серверу, отправку запросов и получение ответов.
 * Использует {@link SocketChannel} для неблокирующего сетевого взаимодействия.
 * Поддерживает автоматические повторные попытки подключения при недоступности сервера.
 * @see Request
 * @see Response
 * @see SocketChannel
 */

public class ClientNetworkManager {
    private Socket socket;
    private final String host;
    private final int port;


    public ClientNetworkManager(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Устанавливает соединение с сервером с автоматическими повторными попытками.
     * При неудачной попытке подключения выводит сообщение об ошибке
     * и повторяет попытку через 3 секунды. Продолжает попытки до успешного
     * подключения или прерывания потока.
     *
     * @throws RuntimeException если поток прерван во время ожидания подключения
     */

    public void connect() {
        System.out.println("Подключение к серверу " + host + ":" + port + "...");

        while (true) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port));

                System.out.println("Подключено к серверу " + host + ":" + port);
                return;

            } catch (IOException e) {
                System.out.println("Сервер временно не доступен");
                System.out.println("Повторная попытка через 3 секунды");
            }
            try {
                Thread.sleep(3000);  // Ждём 3 секунды
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println("Прервано");
                return;
            }
        }
    }

    /**
     * Отправляет запрос на сервер и возвращает ответ.
     * Сериализует объект {@link Request} в байтовый буфер, отправляет его
     * через сетевой канал, затем читает и десериализует ответ {@link Response}.
     *
     * @param request объект запроса, содержащий команду, аргументы и данные
     * @return объект ответа от сервера с результатом выполнения команды
     * @throws IOException если произошла ошибка при чтении/записи в канал
     * @throws ClassNotFoundException если не удалось десериализовать ответ
     */

//    public Response sendRequest(Request request) throws IOException, ClassNotFoundException {
//        OutputStream out = socket.getOutputStream();
//        InputStream in = socket.getInputStream();
//
//        SerializationUtil.serializeToStream(request, out);
//        return (Response) SerializationUtil.deserializeFromStream(in);
//    }
    public Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        // 🔹 Ключевое исправление: проверка и автопереподключение
        if (socket == null || socket.isClosed()) {
            System.out.println("[Network] Соединение разорвано или не установлено. Переподключение...");
            connect();
        }

        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        try {
            SerializationUtil.serializeToStream(request, out);
            out.flush(); // Гарантируем отправку данных в сеть
            return (Response) SerializationUtil.deserializeFromStream(in);
        } catch (SocketException e) {
            // Если сокет закрылся во время операции, помечаем его для переподключения
            System.err.println("[Network] Соединение потеряно во время обмена: " + e.getMessage());
            if (socket != null) {
                try { socket.close(); } catch (IOException ignored) {}
            }
            throw new IOException("Соединение закрыто. Попробуйте снова.", e);
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    /**
     * Закрывает соединение с сервером и освобождает ресурсы.
     * Корректно закрывает сетевой канал, если он был открыт.
     * Рекомендуется вызывать после завершения работы клиента.
     *
     * @throws IOException если произошла ошибка при закрытии канала
     */

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        System.out.println("Отключено от сервера");
    }


}