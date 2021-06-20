package com.itwyx.chatroom.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 *
 * @author 王奕霄
 * @Description:多人聊天室
 * @date 2021年6月18日
 */

public class ServerThread {
    //服务器监听窗口
    private final ServerSocket serverSocket;
    //定义服务器套接字
    //创建动态线程池，适合小并发量，容易出现OutOfMemoryError
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<Socket, String> users = new ConcurrentHashMap();

    public ServerThread() throws IOException {
        serverSocket = new ServerSocket(8099);
        int port = 8099;
        System.out.println("服务器正在启动监听" + port + "端口");

    }


    //多客户可以同时与多用户建立通信连接
    public void Service() throws IOException {
        while (true) {
            Socket socket;
            //未接收到时堵塞
            socket = serverSocket.accept();
            //将服务器和客户端的通信交给线程池处理
            Handler handler = new Handler(socket);
            executorService.execute(handler);
        }
    }


    class Handler implements Runnable {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private void sendToMembers(String msg, String hostAddress, Socket mySocket) throws IOException {

            PrintWriter pw;
            OutputStream out;
            for (Map.Entry<Socket, String> socketStringEntry : users.entrySet()) {
                Map.Entry entry = socketStringEntry;
                Socket tempSocket = (Socket) entry.getKey();
                if (!tempSocket.equals(mySocket)) {
                    out = tempSocket.getOutputStream();
                    pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
                    pw.println(hostAddress + " say：" + msg);
                }
            }

        }

        private void sendToMember(String msg, String hostAddress, Socket another) throws IOException {

            PrintWriter pw;
            OutputStream out;

            for (Map.Entry<Socket, String> socketStringEntry : users.entrySet()) {

                Map.Entry entry = socketStringEntry;
                Socket tempSocket = (Socket) entry.getKey();
                if (tempSocket.equals(another)) {
                    out = tempSocket.getOutputStream();
                    pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
                    pw.println(hostAddress + " private chat to you：" + msg);
                }
            }
        }

        //判断用户是否已经下线
        private boolean isLeaved(Socket temp) {
            boolean leave = true;
            for (Map.Entry<Socket, String> mapEntry : users.entrySet()) {
                if (mapEntry.getKey().equals(temp)) {
                    leave = false;
                    break;
                }
            }
            return leave;
        }

        private PrintWriter getWriter(Socket socket) throws IOException {
            //获得输出流缓冲区的地址
            OutputStream socketOut = socket.getOutputStream();
            //网络流写出需要使用flush，这里在printWriter构造方法直接设置为自动flush
            return new PrintWriter(new OutputStreamWriter(socketOut, StandardCharsets.UTF_8), true);
        }

        private BufferedReader getReader(Socket socket) throws IOException {
            //获得输入流缓冲区的地址
            InputStream socketIn = socket.getInputStream();
            return new BufferedReader(new InputStreamReader(socketIn, StandardCharsets.UTF_8));
        }

        @Override
        public void run() {
            //本地服务器控制台显示客户端连接的用户信息
            System.out.println("New connection has accessed:" + socket.getInetAddress().getHostAddress());
            try {
                BufferedReader br = getReader(socket);
                PrintWriter pw = getWriter(socket);

                pw.println("From Server：欢迎使用多人聊天室服务！");
                pw.println("请输入用户名：");
                String localName = null;
                String hostName = null;

                while ((hostName = br.readLine()) != null) {

                    String finalHostName = hostName;
                    AtomicBoolean flag = new AtomicBoolean(false);
                    users.forEach((k, v) -> {
                        if (v.equals(finalHostName)) {
                            //线程修改了全局变量
                            flag.set(true);
                        }
                    });

                    if (!flag.get()) {
                        localName = hostName;
                        users.put(socket, hostName);
                        //找出不一致问题
                        flag.set(false);
                        break;
                    } else {
                        flag.set(false);
                        pw.println("该用户名已存在，请修改！");
                    }
                }


                Scanner in = new Scanner(System.in);
                System.out.println("请输入要强制下线的用户名");
                String name01 = in.next();
                if (!"null".equals(name01)) {
                    //加当前用户名
                    System.out.println("用户" + name01 + "离开。");
                    Socket temp01 = null;
                    for (Map.Entry<Socket, String> mapEntry : users.entrySet()) {
                        if (mapEntry.getValue().equals(name01)) {
                            temp01 = mapEntry.getKey();
                        }
                    }
                    sendToMembers("我下线了", name01, temp01);
                    sendToMember("服务器已断开连接，结束服务！", name01, temp01);
                    users.remove(temp01, name01);
                }


//                System.out.println(hostName+": "+socket);
                sendToMembers("我已上线", localName, socket);
                System.out.println("当前在线用户:");
                for (Map.Entry<Socket, String> mapEntry : users.entrySet()) {
                    if (mapEntry.getValue() != null) {
                        System.out.print(mapEntry.getValue() + "  ");
                    }
                }
                System.out.print("\n");
                pw.println("输入命令功能：" + "\n" +
                        "(1)S(Select):查看当前上线用户;" + "\n" +
                        "(2)G(Group):群聊;" + "\n" +
                        "(3)O(One-to-one):私信;" + "\n" +
                        "(4)E(Exit):退出当前聊天状态(私信或群组退出);" + "\n" +
                        "(5)bye(bye):离线;" + "\n" +
                        "(6)H(Help):帮助");

                String msg = null;
                //用户连接服务器上线，进入聊天选择状态
                while ((msg = br.readLine()) != null) {
                    if ("bye".equalsIgnoreCase(msg.trim())) {
                        pw.println("From Server：服务器已断开连接，结束服务！");

                        //加当前用户名
                        System.out.println("用户" + localName + "离开。");

                        users.remove(socket, localName);

                        sendToMembers("我下线了", localName, socket);

                        System.out.println("当前在线用户:");
                        for (Map.Entry<Socket, String> mapEntry : users.entrySet()) {
                            if (mapEntry.getValue() != null) {
                                System.out.print(mapEntry.getValue() + "  ");
                            }
                        }

                        System.out.print("\n");

                        break;
                    } else if ("H".equalsIgnoreCase(msg.trim())) {
                        pw.println("输入命令功能：" + "\n" +
                                "(1)S(Select):查看当前上线用户;" + "\n" +
                                "(2)G(Group):群聊;" + "\n" +
                                "(3)O(One-to-one):私信;" + "\n" +
                                "(4)E(Exit):退出当前聊天状态(私信或群组退出);" + "\n" +
                                "(5)bye(bye):离线;" + "\n" +
                                "(6)H(Help):帮助");
                    } else if ("S".equalsIgnoreCase(msg.trim())) {
                        users.forEach((k, v) -> {
                            pw.println("用户:" + v);
                        });
                    }
                    //一对一私聊
                    else if ("O".equalsIgnoreCase(msg.trim())) {
                        pw.println("请输入私信人的用户名：");

                        String name = br.readLine();

                        pw.println("请输入发送的消息");
                        //查找map中匹配的socket，与之建立通信
                        AtomicBoolean isExist = new AtomicBoolean(false);
                        users.forEach((k, v) -> {
                            if (v.equals(name)) {
                                //全局变量与线程修改问题
                                isExist.set(true);
                            }

                        });
                        //对用户不存在的处理逻辑
                        Socket temp = null;
                        for (Map.Entry<Socket, String> mapEntry : users.entrySet()) {
                            if (mapEntry.getValue().equals(name)) {
                                temp = mapEntry.getKey();
                            }
                        }
                        //返回布尔类型的当前值
                        if (isExist.get()) {
                            isExist.set(false);
                            //私信后有一方用户离开，另一方未知，仍然发信息而未收到回复，未处理这种情况
                            while ((msg = br.readLine()) != null) {
                                if (!"E".equals(msg) && !isLeaved(temp)) {
                                    sendToMember(msg, localName, temp);
                                } else if (isLeaved(temp)) {
                                    pw.println("对方已经离开，已断开连接！");
                                    break;
                                } else {
                                    pw.println("您已退出私信模式！");
                                    break;
                                }
                            }
                        } else {
                            pw.println("用户不存在！");
                        }
                    }
                    //选择群聊
                    else if ("G".equals(msg.trim())) {
                        pw.println("您已进入群聊。");
                        pw.println("请输入发送的消息");
                        while ((msg = br.readLine()) != null) {
                            if (!"E".equals(msg) && users.size() != 1) {
                                sendToMembers(msg, localName, socket);
                            } else if (users.size() == 1) {
                                pw.println("当前群聊无其他用户在线，已自动退出！");
                                break;
                            } else {
                                pw.println("您已退出群组聊天室！");
                                break;
                            }
                        }

                    } else {
                        pw.println("请选择聊天状态！");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new ServerThread().Service();
    }

}
 
 