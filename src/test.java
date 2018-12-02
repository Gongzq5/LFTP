import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import lftp.LFTP_head;

public class test {

	public test() {
		// TODO Auto-generated constructor stub
	}
	
	public class SendThread extends Thread {
		@Override
		public void run() {
			try {
				DatagramSocket datagramSocket = new DatagramSocket();
				System.out.println(datagramSocket.getLocalPort());
				DatagramPacket p = new DatagramPacket("Hello".getBytes(), "Hello".length(),
						InetAddress.getLocalHost(), 2014);
				System.out.println(datagramSocket.getLocalPort());
				System.out.println(datagramSocket.getLocalPort());
				datagramSocket.send(p);
				System.out.println(datagramSocket.getLocalPort());
				System.out.println(datagramSocket.getPort());
				datagramSocket.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public class ReceiveThread extends Thread {
		@Override
		public void run() {
			try {
				byte[] buf = new byte[1024];
				DatagramSocket datagramSocket = new DatagramSocket(2014);
				DatagramPacket datagramPacket = new DatagramPacket(buf, 200);
				datagramSocket.receive(datagramPacket);
				System.out.println("RECEIVE " + datagramPacket.getData().toString() + " from address " + datagramPacket.getAddress() 
							+ " from port " + datagramPacket.getPort());
				datagramSocket.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	public static String bytesToHexFun3(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for(byte b : bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", new Integer(b & 0xff)));
        }

        return buf.toString();
    }
	
	public static void main(String[] args) {
//		test t = new test();
//		t.new SendThread().start();
//		t.new ReceiveThread().start();

		
	}
}
