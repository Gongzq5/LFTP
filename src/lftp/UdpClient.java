package lftp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpClient {
	   
    private String sendStr = "SendString 发送";
    private String netAddress = "127.0.0.1";
    private final int PORT_NUM = 5066;
    private static final int TIMEOUT = 3000; // 超时时间
    private static final int MAXTRIES = 5;     // 最大重发次数5次  
    private static final int LFTP_length = 4096;
   
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
   
    public UdpClient(){
        try {
           
            /*** 发送数据***/
            // 初始化datagramSocket,注意与前面Server端实现的差别
            datagramSocket = new DatagramSocket();
            // 使用DatagramPacket(byte buf[], int length, InetAddress address, int port)函数组装发送UDP数据报
            byte[] buf = sendStr.getBytes();
            InetAddress address = InetAddress.getByName(netAddress);
            datagramPacket = new DatagramPacket(buf, buf.length, address, PORT_NUM);          
            datagramSocket.setSoTimeout(TIMEOUT);
            
            /*** 接收数据***/
            byte[] receBuf = new byte[LFTP_length];
            DatagramPacket recePacket = new DatagramPacket(receBuf, receBuf.length);
            
            int tries = 0;
            boolean receivedResponse = false;  
            
            // 发送数据，超时没有收到ACK重发
            do {
            	// 发送数据
                datagramSocket.send(datagramPacket);
                System.out.println("Send: " + sendStr);
                tries ++;
	            try {
//	            	DatagramSocket datagramSocket2 = new DatagramSocket(PORT_NUM);
	            	datagramSocket.receive(recePacket);
//	            	datagramSocket2.close();
	            	receivedResponse = true;
	                String receStr = new String(recePacket.getData(), 0 , recePacket.getLength());
	                System.out.println("Client Rece Ack:" + receStr);
	                System.out.println(recePacket.getPort());
	            } catch (InterruptedIOException e) { // 当receive不到信息或者receive时间超过3秒时，就向服务器重发请求    
	            	System.out.println("Timed out : " + TIMEOUT + " Times: " + tries);              	           
	            } 
            } while(tries < MAXTRIES && !receivedResponse);                  
           
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭socket
            if(datagramSocket != null){
                datagramSocket.close();
                
            }
        }
    }  
    public static void main(String[] args) throws IOException {
    	new MyThreadClient().start();
    }

}
class MyThreadClient extends Thread {
    
    private int i = 0;
    @Override
    public void run() {
        for (i = 0; i < 5; i++) {
        	System.out.println("run: " + i);
        	new UdpClient();
        }
    }
}