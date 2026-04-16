package org.example.server.network;

import org.example.common.Request;
import org.example.common.Response;
import org.example.common.util.SerializationUtil;
import org.example.server.handlers.RequestHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ServerNetworkManager {
    private ServerSocketChannel serverChannel;
    private int port;
    private Selector selector;
    private static final int BUFFER_SIZE = 8192;

    public ServerNetworkManager(int port) {
        this.port = port;
    }

    public void start(RequestHandler handler){
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Сервер запущен на порту " + port);
            System.out.println("Ожидание подключений...");

            while (true) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        }
                        if (key.isReadable()) {
                            handleRead(key, handler);
                        }
                    } catch (Exception e) {
                        System.err.println("Ошибка обработки: " + e.getMessage());
                        e.printStackTrace();
                        key.cancel();
                        try { key.channel().close(); } catch (IOException ex) {}
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException{
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(BUFFER_SIZE));
            System.out.println("Новый клиент: " + clientChannel.getRemoteAddress());
        }
    }

    private void handleRead(SelectionKey key, RequestHandler handler) throws IOException, ClassNotFoundException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        int bytesRead = clientChannel.read(buffer);
        if (bytesRead == -1) {
            key.cancel();
            clientChannel.close();
            System.out.println("Клиент отключён");
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();

            while (true) {

                if (buffer.remaining() < 4) {
                    break;
                }

                buffer.mark();
                int length = buffer.getInt();

                if (buffer.remaining() >= length) {

                    buffer.reset();

                    Request request = (Request) SerializationUtil.deserialize(buffer);
                    System.out.println("Получен запрос: " + request.getName());

                    Response response = handler.handle(request);

                    ByteBuffer responseBuffer = SerializationUtil.serialize(response);
                    while (responseBuffer.hasRemaining()) {
                        clientChannel.write(responseBuffer);
                    }
                    System.out.println("Ответ отправлен: " + response.getMessage());

                } else {
                    buffer.reset();
                    break;
                }
            }
            buffer.compact();
        }
    }


    public void stop(){
        try {
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
            System.out.println("Сервер остановлен");
        } catch (IOException e) {
            System.err.println("Ошибка при остановке: " + e.getMessage());
        }
    }
}
