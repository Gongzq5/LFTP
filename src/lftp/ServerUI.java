package lftp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ServerUI {	
	private final int PORT_NUM = 5066;
	private DatagramSocket datagramSocket = null;
	private int timeOut = 3000;
	private Timer timer = null;
	private Map<InetAddress, Integer> address2port = new HashMap<InetAddress, Integer>();
	private Map<InetAddress, String> address2path = new HashMap<InetAddress, String>();
	private Map<InetAddress, Long> address2sendtime = new HashMap<InetAddress, Long>();
	
	private Map<InetAddress, DatagramSocket> address2Socket = new HashMap<InetAddress, DatagramSocket>();
	private Map<InetAddress, Long> address2receivetime = new HashMap<InetAddress, Long>();
	
	public ServerUI() throws IOException {
		datagramSocket = new DatagramSocket(PORT_NUM);
		byte[] receMsgs = new byte[1024];
		DatagramPacket datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
		timer = new Timer();
		timer.schedule(new TimeCounter(), 0, 10);

		while(true) {
			System.out.println("wait");
			datagramSocket.receive(datagramPacket);								
			String receStr = new String(datagramPacket.getData(), 0 , datagramPacket.getLength());
			byte[] hashcode = new byte[4];
			System.arraycopy(receMsgs, 3, hashcode, 0, 4);		
					
			int length = (int)receMsgs[7];
			byte[] pathByte = new byte[length];
			System.arraycopy(receMsgs, 8, pathByte, 0, length);			
			String sendpath = new String(pathByte);
			
			System.out.println("length " + length + " path: " + sendpath);
			
			if (receStr.substring(0, 3).equals("GET")) {
				this.sendFile(sendpath, datagramPacket.getAddress(), datagramPacket.getPort(), hashcode);
			} else if (receStr.substring(0, 3).equals("SED")){
				this.receiveFile(sendpath, datagramPacket.getAddress(), datagramPacket.getPort(), hashcode);
			}
		}
	}
	
	public void sendFile(String path, InetAddress address, int port, byte[] hashcode) {
		if (address2sendtime.containsKey(address)) {
			System.out.println("I have receive GET message before");
			address2sendtime.put(address, System.currentTimeMillis());			
		} else {
			address2path.put(address, path);					
			address2port.put(address, port);			
			address2sendtime.put(address, System.currentTimeMillis());						
		}	
		System.out.println(" path: " + path);
		
		
		byte[] buf = new byte[1024];
		System.arraycopy("ACK".getBytes(), 0, buf, 0, "ACK".getBytes().length);
		System.arraycopy(hashcode, 0, buf, "ACK".getBytes().length, 4);
		System.arraycopy(LFTP_head.IntToByte(port), 0, buf, "ACK".getBytes().length+4, 4);
		
		
        DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, port);
        
        try {
			datagramSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void receiveFile(String path, InetAddress address, int port, byte[] hashcode) {		
		if (address2receivetime.containsKey(address)) {
			System.out.println("I have receive SEND message before");
			address2receivetime.put(address, System.currentTimeMillis());			
		} else {
			address2path.put(address, path);		
			System.out.println(" path: " + path);
			
			DatagramSocket ds = null;			
			try {
				ds = new DatagramSocket();
			} catch (SocketException e) {
				
			}	
			address2Socket.put(address, ds);			
			address2receivetime.put(address, System.currentTimeMillis());
			
			System.out.println(ds.getLocalPort());
		}
			
		byte[] buf = new byte[1024];
		System.arraycopy("ACK".getBytes(), 0, buf, 0, "ACK".getBytes().length);
		System.arraycopy(hashcode, 0, buf, "ACK".getBytes().length, 4);
		System.arraycopy(LFTP_head.IntToByte(address2Socket.get(address).getLocalPort()), 0, buf, "ACK".getBytes().length+4, 4);			

        DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, port);
        try {
			datagramSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private class TimeCounter extends TimerTask {
		@Override
		public void run() {
			long curr = System.currentTimeMillis();
			for (InetAddress address : address2sendtime.keySet()) {
				if (curr - address2sendtime.get(address) > timeOut) {					
		    		try {
		    			new send(address, address2port.get(address), address2path.get(address)).start();;
//						new SendService(address, address2port.get(address), address2path.get(address)).send();
						System.out.println("send over");
					} catch (Exception e) {
						e.printStackTrace();
					}
		    		
		    		address2path.remove(address);
		    		address2port.remove(address);
		    		address2sendtime.remove(address);
				}
			}
			
			for (InetAddress address : address2receivetime.keySet()) {
				if (curr - address2receivetime.get(address) > timeOut) {					
		    		try {
//		    			new ReceiveService(address2Socket.get(address), address2path.get(address)).receive();
//		    			new ReceiveService(address2Socket.get(address), "test\\dst10m.txt").receive();
		    			new receive(address2Socket.get(address), address2path.get(address)).start();
		    			System.out.println("receive over");
		    		} catch (Exception e) {
						e.printStackTrace();
					}
		    		
		    		address2path.remove(address);
		    		address2receivetime.remove(address);
				}
			}

		}
	}
	
	public class send extends Thread {
		InetAddress address = null;
		int port = 0;
		String path;
		
		public send (InetAddress address, int port, String path) {
			this.address = address;
			this.port = port;
			this.path = path;
		}
		@Override    
	    public void run() {
			try {
				new SendService(address, port, path).send();
			} catch (UnknownHostException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public class receive extends Thread {
		DatagramSocket socket = null;
		String path;
		
		public receive (DatagramSocket socket, String path) {
			this.socket = socket;
			this.path = path;
		}
		@Override    
	    public void run() {
			try {
				new ReceiveService(socket, path).receive();
			} catch (UnknownHostException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) throws IOException {

		new ServerUI();		
	}
	
}