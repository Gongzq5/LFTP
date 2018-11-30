package lftp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpClient {
	   
    private String sendStr = "SendString ����";
    private String netAddress = "127.0.0.1";
    private final int PORT_NUM = 5066;
    private static final int TIMEOUT = 3000; // ��ʱʱ��
    private static final int MAXTRIES = 5;     // ����ط�����5��  
    private static final int LFTP_length = 4096;
   
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
   
    public UdpClient(){
        try {
           
            /*** ��������***/
            // ��ʼ��datagramSocket,ע����ǰ��Server��ʵ�ֵĲ��
            datagramSocket = new DatagramSocket();
            // ʹ��DatagramPacket(byte buf[], int length, InetAddress address, int port)������װ����UDP���ݱ�
            byte[] buf = sendStr.getBytes();
            InetAddress address = InetAddress.getByName(netAddress);
            datagramPacket = new DatagramPacket(buf, buf.length, address, PORT_NUM);          
            datagramSocket.setSoTimeout(TIMEOUT);
            
            /*** ��������***/
            byte[] receBuf = new byte[LFTP_length];
            DatagramPacket recePacket = new DatagramPacket(receBuf, receBuf.length);
            
            int tries = 0;
            boolean receivedResponse = false;  
            
            // �������ݣ���ʱû���յ�ACK�ط�
            do {
            	// ��������
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
	            } catch (InterruptedIOException e) { // ��receive������Ϣ����receiveʱ�䳬��3��ʱ������������ط�����    
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
            // �ر�socket
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