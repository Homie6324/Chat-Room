package com.itwyx.chatroom.Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created with IntelliJ IDEA.
 *
 * @author 王奕霄
 * @Description:多人聊天室
 * @date 2021年6月18日
 */

public class ClientThread extends Application {

    private final Button btExit = new Button("退出");
    private final Button btSend = new Button("发送");
    private final Button resourceCode = new Button("源码");
    private final Button blog = new Button("博客");
    //private Button kickOut = new Button("踢人");

    //输入信息区域
    private final TextField tfSend = new TextField();

    //显示区域
    private final TextArea taDisplay = new TextArea();


    //填写ip地址
    public final TextField ipAddress = new TextField();

    //填写端口
    private final TextField port = new TextField();


    private final Button btConn = new Button("连接");
    private Client client;
    private Thread readThread;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane mainPane = new BorderPane();
//        Image image = new Image("file:/com/itwyx/chatroom/img/background2.png");
//        BackgroundImage backgroundImage = new BackgroundImage(image,null,null,null,null);
//        btConn.setBackground(new Background(backgroundImage));
        BackgroundImage myBI = new BackgroundImage(new Image("file:/com/itwyx/chatroom/img/background.png", 32, 32, false, true), BackgroundRepeat.REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT);
        mainPane.setBackground(new Background(myBI));
//        BackgroundFill myBF = new BackgroundFill(Color.gray(100),new CornerRadii(1),
//                new Insets(0.0,0.0,0.0,0.0));
//
//        mainPane.setBackground(new Background(myBF));
        //mainPane.setStyle("-fx-text-fill: blueviolet");
        primaryStage.setTitle("我的聊天室");
        taDisplay.setStyle("-fx-text-fill:darkgoldenrod");
        //连接服务器区域
        HBox hBox01 = new HBox();
        //设置字间距
        hBox01.setSpacing(10);
        //设置内边距
        hBox01.setPadding(new Insets(10, 20, 10, 20));
        //设置对齐方式
        hBox01.setAlignment(Pos.CENTER);
        hBox01.getChildren().addAll(new Label("IP地址："), ipAddress, new Label("端口："), port, btConn);
        mainPane.setTop(hBox01);

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.setPadding(new Insets(10, 20, 10, 20));
        vBox.getChildren().addAll(new Label("消息记录"), taDisplay, new Label("发送消息"), tfSend);
        //设置布局自适应
        VBox.setVgrow(taDisplay, Priority.ALWAYS);
        mainPane.setCenter(vBox);


        HBox hBox02 = new HBox();
        //设置字间距
        hBox02.setSpacing(10);
        //设置内边距
        hBox02.setPadding(new Insets(10, 20, 10, 20));
        //设置对齐方式
        hBox02.setAlignment(Pos.CENTER_RIGHT);
        hBox02.getChildren().addAll(btSend, btExit, resourceCode, blog);
        mainPane.setBottom(hBox02);

//        HBox hBox03 = new HBox();
//        hBox03.setSpacing(10);
//        hBox03.setPadding(new Insets(10, 20, 10, 20));
//        hBox03.setAlignment(Pos.CENTER_LEFT);
//        hBox03.getChildren().addAll(resoucreCode,blog);
//        mainPane.setBottom(hBox03);

        Scene scene = new Scene(mainPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        //连接服务器之前，发送bye后禁用发送按钮，禁用Enter发送信息输入区域，禁用下载按钮
        btSend.setDisable(true);
        tfSend.setDisable(true);

        //源码按钮
        resourceCode.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/Homie6324/Chat-Room"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        });

        //我的博客按钮
        blog.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI("http://flash-shop.gitee.io/"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        });

        //连接按钮
        btConn.setOnAction(event -> {
            String ip = ipAddress.getText().trim();
            String port = this.port.getText().trim();
            System.out.println("输入的IP地址" + ip);

            try {
                //client是本程序定义的一个Client类型的成员变量
                client = new Client(ip, port);
                //用于接收服务器信息的单独线程
                readThread = new Thread(() -> {
                    //从服务器接收一串字符
                    String receiveMsg = null;
                    while ((receiveMsg = client.receive()) != null) {
                        //lambda表达式不能直接访问外部非final类型局部变量，需要定义一个临时变量
                        String msgTemp = receiveMsg;
                        //收消息并显示
                        //在非Fx线程要执行Fx线程相关的任务，必须在 Platform.runlater 中执行
                        Platform.runLater(() -> taDisplay.appendText(msgTemp + "\n"));
                    }
                    Platform.runLater(() -> {
                        taDisplay.appendText("对话已关闭！\n即将自动退出");
                        try {
                            exit();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                });

                readThread.start();
                //连接服务器之后未结束服务前禁用再次连接
                btConn.setDisable(true);
                //重新连接服务器时启用输入发送功能
                tfSend.setDisable(false);
                btSend.setDisable(false);
            } catch (Exception e) {
                taDisplay.appendText("服务器连接失败！" + e.getMessage() + "\n");
            }
        });

//        btConn.defaultButtonProperty();

        //发送按钮事件
        btSend.setOnAction(event -> {
            String msg = tfSend.getText();
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
            //向服务器发送一串字符
            client.send(msg);
            taDisplay.appendText(sdf.format(now) + "\n");
            taDisplay.appendText("I say：" + msg + "\n");
            if (msg.equalsIgnoreCase("bye")) {
                //发送bye后禁用发送按钮
                btSend.setDisable(true);
                //禁用Enter发送信息输入区域
                tfSend.setDisable(true);
                //结束服务后再次启用连接按钮
                btConn.setDisable(false);
            }

        });

//        //踢人按钮事件
//        kickOut.setOnAction(event -> {
//            String msg=tfSend.getText();
//            Date now = new Date();
//            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
//            tcpClient.send(msg);//向服务器发送一串字符
//            taDisplay.appendText(sdf.format(now)+"\n");
//            taDisplay.appendText(msg+"已经被强制下线"+"\n");
//            if (msg.equalsIgnoreCase("bye")){
//                btnSend.setDisable(true);//发送bye后禁用发送按钮
//                tfSend.setDisable(true);//禁用Enter发送信息输入区域
//                //结束服务后再次启用连接按钮
//                btConn.setDisable(false);
//            }
//            tfSend.clear();
//        });
        //对输入区域绑定键盘事件
        tfSend.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String msg = tfSend.getText();
                Date now = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
                client.send(msg);//向服务器发送一串字符
                taDisplay.appendText(sdf.format(now) + "\n");
                taDisplay.appendText("I say：" + msg + "\n");

                if ("bye".equalsIgnoreCase(msg)) {
                    //禁用Enter发送信息输入区域
                    tfSend.setDisable(true);
                    //发送bye后禁用发送按钮
                    btSend.setDisable(true);
                    //结束服务后再次启用连接按钮
                    btConn.setDisable(false);
                }
                tfSend.clear();
            }
        });

        //退出按钮的设计
        btExit.setOnAction(event -> {
            try {
                exit();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        //窗体关闭响应的事件,点击右上角的×关闭,客户端也关闭
        primaryStage.setOnCloseRequest(event -> {
            try {
                exit();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void exit() throws InterruptedException {
        if (client != null) {
            client.send("bye");
            Thread.sleep(1500);
            //多线程等待，关闭窗口时还有线程等待IO，设置1s间隔保证所有线程已关闭
            client.close();
            System.out.println("即将自动关闭");
        }
        System.exit(0);
    }
//    public String setIp(){
//        String Ip;
//        Ip=ipAddress.getText().trim();
//        return Ip;
//    }
}