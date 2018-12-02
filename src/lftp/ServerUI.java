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
import java.util.concurrent.SynchronousQueue;

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
			int length = (int)receMsgs[3];
			byte[] pathByte = new byte[length];
			System.arraycopy(receMsgs, 4, pathByte, 0, length);			
			String sendpath = new String(pathByte);
			
			System.out.println("length " + length + " path: " + sendpath);
			
			if (receStr.substring(0, 3).equals("GET")) {
				this.sendFile(sendpath, datagramPacket.getAddress(), datagramPacket.getPort());
			} else if (receStr.substring(0, 3).equals("SED")){
				this.receiveFile(sendpath, datagramPacket.getAddress(), datagramPacket.getPort());
			}
		}
	}
	
	public void sendFile(String path, InetAddress address, int port) {
		if (address2sendtime.containsKey(address)) {
			address2sendtime.put(address, System.currentTimeMillis());			
		} else {
			address2path.put(address, path);					
			address2port.put(address, port);			
			address2sendtime.put(address, System.currentTimeMillis());						
		}	
		System.out.println(" path: " + path);
		
		
		byte[] buf = new byte[1024];
		System.arraycopy("ACK".getBytes(), 0, buf, 0, "ACK".getBytes().length);
		System.arraycopy(address.getAddress(), 0, buf, "ACK".getBytes().length, 4);
		System.arraycopy(LFTP_head.IntToByte(port), 0, buf, "ACK".getBytes().length+4, 4);
		

		System.out.println(Arrays.toString(address.getAddress()));
		
		byte[] addressByte = new byte[4];
		System.arraycopy(buf, 3, addressByte, 0, 4);
		System.out.println("address " + Arrays.toString(addressByte));
		
        DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, port);
        
        try {
			datagramSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void receiveFile(String path, InetAddress address, int port) {
		if (address2receivetime.containsKey(address)) {
			address2receivetime.put(address, System.currentTimeMillis());			
		} else {
			address2path.put(address, path);		
			System.out.println(" path: " + path);
			
			DatagramSocket ds = null;
			for(int i = 5001; i < 65536; i++){				
				try {
					ds = new DatagramSocket(i);
					break;
				} catch (SocketException e) {
					
				}				
			}
			address2Socket.put(address, ds);
			
			address2receivetime.put(address, System.currentTimeMillis());
			
			String receive = new String("ACK");
			receive += new String(address.getAddress());
			receive += new String(LFTP_head.IntToByte(port));
			receive += new String(LFTP_head.IntToByte(ds.getLocalPort()));
			
			byte[] buf = receive.getBytes();
	        DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, address, port);
	        try {
				datagramSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class TimeCounter extends TimerTask {
		@Override
		public void run() {
			long curr = System.currentTimeMillis();
			for (InetAddress address : address2sendtime.keySet()) {
				if (curr - address2sendtime.get(address) > timeOut) {					
		    		try {
						new SendService(address, address2port.get(address), address2path.get(address)).send();
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
		    			new ReceiveService(address2Socket.get(address), address2path.get(address)).receive();
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
	
	public static void main(String[] args) throws IOException {
//		String str = new String("GET");
//		String str2 = new String("test\\src10m.txt");
//		str += str2.length() + str2;
//		System.out.println(str);
//		new ServerUI().sendFile(str, InetAddress.getLocalHost(), 5066);
		
		new ServerUI();
		
		//ip: 172.18.34.154
	}
	
}



//    		
//    		for(int i = 5001; i < 65536; i++){				
//				try {
//					DatagramSocket ds=new DatagramSocket(i);
////					ds.close();
//					port = i;
//					break;
//				} catch (SocketException e) {
//					
//				}				
//			}
//    		
//    		byte[] buf2 = "I receive the message".getBytes();
//            DatagramPacket sendPacket2 = new DatagramPacket(buf2, buf2.length, datagramPacket.getAddress(), datagramPacket.getPort());
//            datagramSocket.send(sendPacket);
//            
//            DatagramSocket receiveSocket = new DatagramSocket(5066);   		
//            new ReceiveService(receiveSocket, "test\\dst10m.txt");
