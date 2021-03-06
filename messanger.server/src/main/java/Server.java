import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server implements TCPConnectionListener{

    public static void main(String[] args) {
        new Server();
    }

    private final Map<String, TCPConnection> connection = new HashMap<String, TCPConnection>();
    DataBase dataBase = DataBase.getInstance();

    private Server() {
        System.out.println("Server running...");
        try (ServerSocket serverSocket = new ServerSocket(443)) {
            while (true) {
                try {
                    new TCPConnection(this, serverSocket.accept());
                }catch (IOException e) {
                    System.out.println("TCPConnection exception: " + e);
                }
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onConnectionReady(String login, TCPConnection tcpConnection) {
        connection.put(login, tcpConnection);
        System.out.println(login + ": " + tcpConnection);

        System.out.println(connection);

    }

    @Override
    public void addNewMessage(String sender, String recipient, String msg) {

        // эта фуекция должна вернуть id и time записанного сообщения
        String[] arr = dataBase.insertMessage(sender, recipient, msg);

        // отправляем это сообщене отправителю в виде | id | sender | recipient | msg | time |
        sendingMessage(sender, arr[0], sender, recipient, msg, arr[1], "false");

        if((connection.get(recipient) == null) || recipient.equals(sender)) return;

        // отправляем это сообщене получателю в виде | id | sender | recipient | msg | time |
        sendingMessage(recipient, arr[0], sender, recipient, msg, arr[1], "false");
    }

    // нужно протестировать
    @Override
    public void listOfMessages(String user, String time) {
        System.out.println("LIST OF MESSAGES");
        System.out.println(user + " " + time);

        // данная функция должна возвращать список сообщений
        ArrayList<String[]> arr = dataBase.getListMessages(user, time);
        String[] column = null;

        // отправка сообщений на клиент
        for(int i = 0; i < arr.size(); ++i) {
            column = arr.get(i);

            System.out.print(user + " | ");
            System.out.print(column[0] + " | ");
            System.out.print(column[1] + " | ");
            System.out.print(column[2] + " | ");
            System.out.print(column[3] + " | ");
            System.out.print(column[4] + " | ");
            System.out.print(column[5] + " | ");
            System.out.println();

            sendingMessage(user, column[0], column[1], column[2], column[3], column[4], column[5]);
        }
    }

    // перед вызовом этой функции нужно отправлять /NEW_MESSAGE
    @Override
    public void sendingMessage(String sendTo, String id, String sender, String recipient, String msg, String time, String flag) {
        sendToUser(sendTo, "/NEW_MESSAGE");

        sendToUser(sendTo, id);
        sendToUser(sendTo, sender);
        sendToUser(sendTo, recipient);
        sendToUser(sendTo, msg);
        sendToUser(sendTo, time);
        sendToUser(sendTo, flag);
    }

    // работает не правильно
    @Override
    public void onDisconnect(TCPConnection tcpConnection, String name) {
        System.out.println("OnDisconnect");
        connection.remove(name);
        System.out.println(connection);
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void setFlagTrueById(String id) {
        dataBase.setFlagTrueById(Integer.parseInt(id));
    }

    @Override
    public void setFlagTrueByTime(String user1, String user2, String time) {
        dataBase.setFlagTrueByTime(user1, user2, time);
    }

    // протестировать
    @Override
    public boolean signUp(String login, String password) {
        // проверить существование пользователя с данным именем(login)
        if (!dataBase.isExist(login)) {
            // регистрация пользоватея и отправка на клиент (true)
            if (dataBase.newUserRegistration(login, password)) {
                // отправить на клиент true
                return true;
            }
        }
            // отправляем на клиент false
            return false;
    }

    @Override
    public boolean logIn(String login, String password) {

        if (dataBase.isExist(login)) {
            // сравнить авпроль и логин
            String[] arr = dataBase.logIn(login);
            if (arr[0].equals(login) && arr[1].equals(password)) {
                return true;
            }
        }
        return false;
    }

    private void sendToUser(String recipient, String msg) {
        TCPConnection tcpConnection = connection.get(recipient);

        if (!(connection.get(recipient) == null)) {
            connection.get(recipient).sendString(msg);
        }

    }

}
