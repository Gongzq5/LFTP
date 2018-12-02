package useless;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpServer {
    // 定义一些常量
    private final int MAX_LENGTH = 1024; // 最大接收字节长度
    private final int PORT_NUM   = 5066;   // port号
    // 用以存放接收数据的字节数组
    private byte[] receMsgs = new byte[MAX_LENGTH];
    // 数据报套接字
    private DatagramSocket datagramSocket;
    // 用以接收数据报
    private DatagramPacket datagramPacket;
   
    public UdpServer(){
        try {
            /******* 接收数据流程**/
            // 创建一个数据报套接字，并将其绑定到指定port上
            datagramSocket = new DatagramSocket(PORT_NUM);
            // DatagramPacket(byte buf[], int length),建立一个字节数组来接收UDP包
            datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
            // receive()来等待接收UDP数据报
            System.out.println("wait for receive");

            datagramSocket.receive(datagramPacket);
           
            /****** 解析数据报****/
            String receStr = new String(datagramPacket.getData(), 0 , datagramPacket.getLength());
            System.out.println("Server Rece:" + receStr);
            System.out.println("Server Port:" + datagramPacket.getPort());
            System.out.println("Server Address:" + datagramPacket.getAddress());
            
            /***** 返回ACK消息数据报*/
            // 组装数据报
            byte[] buf = "I receive the message".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, datagramPacket.getAddress(), datagramPacket.getPort());
            // 发送消息
            datagramSocket.send(sendPacket);
 
            
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭socket
            if (datagramSocket != null) {
                datagramSocket.close();
            }
        }
    }
    
    
    
    public static void main(String[] args) throws IOException {
    	new MyThreadServer().start();
    }
}

class MyThreadServer extends Thread {
    
    private int i = 0;
    @Override
    public void run() {
        for (i = 0; i < 5; i++) {
        	System.out.println("run: " + i);
        	new UdpServer();
        }
    }
}