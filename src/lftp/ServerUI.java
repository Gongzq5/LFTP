package lftp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyStore.PrivateKeyEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ServerUI {	
	private final int PORT_NUM = 5066;
	private DatagramSocket datagramSocket = null;
	private int timeOut = 3000;
	private Timer timer = null;
	private Map<Integer, Integer> hash2port = new HashMap<Integer, Integer>();
	private Map<Integer, InetAddress> hash2address = new HashMap<Integer, InetAddress>();
	private Map<Integer, String> hash2path = new HashMap<Integer, String>();	
	private Map<Integer, Long> hash2sendtime = new HashMap<Integer, Long>();
	private Map<Integer, SendService> hash2sendService = new HashMap<Integer, SendService>();

	private Map<Integer, DatagramSocket> hash2Socket = new HashMap<Integer, DatagramSocket>();
	private Map<Integer, Long> hash2receivetime = new HashMap<Integer, Long>();
	
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
			} else if (receStr.substring(0, 3).equals("HRT")) {
				this.heart(datagramPacket.getAddress(), datagramPacket.getPort(), hashcode);
			}
		}
	}
	
	public void sendFile(String path, InetAddress address, int port, byte[] hashcode) {
		int hash = LFTP_head.Byte2Int(hashcode);
		System.out.println("hash:    " + hash);
		
		if (hash2sendtime.containsKey(hash)) {
			System.out.println("I have receive GET message before");
			hash2sendtime.put(hash, System.currentTimeMillis());			
		} else {
			hash2path.put(hash, path);
			hash2address.put(hash, address);
			hash2port.put(hash, port);			
			hash2sendtime.put(hash, System.currentTimeMillis());						
		}	

		System.out.println("[Server UI] ACK path: " + path);
		System.out.println("[Server UI] ACK address£º " + address.getHostAddress());
		System.out.println("[Server UI] ACK port: " + port);

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
		int hash = LFTP_head.Byte2Int(hashcode);
		
		if (hash2receivetime.containsKey(address)) {
			System.out.println("I have receive SEND message before");
			hash2receivetime.put(hash, System.currentTimeMillis());			
		} else {
			hash2path.put(hash, path);		
			System.out.println(" path: " + path);
			
			DatagramSocket ds = null;			
			try {
				ds = new DatagramSocket();
			} catch (SocketException e) {
				
			}	
			hash2Socket.put(hash, ds);			
			hash2receivetime.put(hash, System.currentTimeMillis());
			hash2address.put(hash, address);
			
			System.out.println(ds.getLocalPort());
		}
			
		byte[] buf = new byte[1024];
		System.arraycopy("ACK".getBytes(), 0, buf, 0, "ACK".getBytes().length);
		System.arraycopy(hashcode, 0, buf, "ACK".getBytes().length, 4);
		System.arraycopy(LFTP_head.IntToByte(hash2Socket.get(hash).getLocalPort()), 0, buf, "ACK".getBytes().length+4, 4);			

        DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, port);
        try {
			datagramSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void heart(InetAddress address, int port, byte[] hashcode) {
		int hash = LFTP_head.Byte2Int(hashcode);
		if (hash2port.get(hash) == port && hash2address.get(hash).equals(address) ) {
			System.out.println("your port has not change");
		} else {
			hash2address.put(hash, address);
			hash2port.put(hash, port);
			hash2sendService.get(hash).change(address, port);  
			System.out.println("your port has changed");
		}
	}
	
	private class TimeCounter extends TimerTask {
		@Override
		public void run() {

			long curr = System.currentTimeMillis();
			for (Integer hash : hash2sendtime.keySet()) {
				if (curr - hash2sendtime.get(hash) > timeOut) {					
		    		try {
		    			System.out.println("start send " + hash);
		    			new send(hash2address.get(hash), hash2port.get(hash), hash2path.get(hash), hash).start();

						System.out.println("send over");
					} catch (Exception e) {
						e.printStackTrace();
					}
		    		
//		    		hash2path.remove(hash);
//		    		hash2port.remove(hash);
		    		hash2sendtime.remove(hash);
//		    		hash2address.remove(hash);
				}
			}
			
			for (Integer hash : hash2receivetime.keySet()) {
				if (curr - hash2receivetime.get(hash) > timeOut) {					
		    		try {

//		    			new receive(address2Socket.get(address), address2path.get(address)).start();
 	    			    new ReceiveService(hash2Socket.get(hash), "test\\dst12m.txt").receive();
//		    			new receive(hash2Socket.get(hash), hash2path.get(hash)).start();

		    			System.out.println("receive over");
		    		} catch (Exception e) {
						e.printStackTrace();
					}
		    		
		    		hash2path.remove(hash);
		    		hash2receivetime.remove(hash);
		    		hash2address.remove(hash);
		    		hash2Socket.remove(hash);
				}
			}

		}
	}
	
	
	
	public class send extends Thread {
		InetAddress address = null;
		int port = 0;
		String path;
		int hash = 0;
		
		public send (InetAddress address, int port, String path, int hash) {
			this.address = address;
			this.port = port;
			this.path = path;
			this.hash = hash;
		}
		@Override    
	    public void run() {
			try {
				SendService sendService = new SendService(address, port, path);
				hash2sendService.put(hash, sendService);
				System.out.println("send send send");
				sendService.send();
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