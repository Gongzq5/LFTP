package useless;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Server {
	public Server() {}
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Thread t1 = new ServerThread();
		Thread t2 = new ClientThread();
		
		t1.start();
		t2.start();
		
		try {
			Thread.sleep(10000);
			
			t1.join();
			t2.join();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}

class ServerThread extends Thread {
    public ServerThread() {
        
    }

    public void run()  {
    	try {
	    	InetAddress inetAddress = InetAddress.getLocalHost();
			DatagramSocket socket = new DatagramSocket();
			while (true) {
				System.out.println("Server, waiting for 2s...");
				Thread.sleep(2000);
				byte [] data = "Hello, this is server UI's heart jump.".getBytes();
				DatagramPacket datagramPackage = new DatagramPacket(data, data.length, 
						inetAddress, 8080);
				socket.send(datagramPackage);				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}

class ClientThread extends Thread {
    public ClientThread() {}

    public void run()  {
    	try {
	    	InetAddress inetAddress;
			DatagramSocket socket = new DatagramSocket();
			while (true) {
				System.out.println("Client, waiting for message...");
				byte [] data = new byte[1024];
				DatagramPacket p = new DatagramPacket(data, data.length);
				socket.receive(p);
				System.out.println("From:" + p.getAddress());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
