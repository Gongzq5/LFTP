package lftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SendService {
	private static final int LISTSIZE = 256;
	private static final int READSIZE = 100;  //4086
	private static final int HEADSIZE = 10;
	private List<LFTP_packet> packetList = new ArrayList<LFTP_packet>(LISTSIZE);
	private SendThread sendThread = null;
	private RecieveThread recieveThread = null;
	
	private int sendBase = 0;	
	private int fileNumber = 0;
	private int nextSeqNumber = 0;
	private boolean isSending = false;
	private int windowSize = 64;
	
	private String path = null;
	private int port = 8080;
	private InetAddress inetAddress = null;
	
	
	public SendService() {}

	public void send() throws UnknownHostException {
		if (sendThread == null) {
			InetAddress inetAddress = InetAddress.getLocalHost();
			sendThread = new SendThread(inetAddress);
			isSending = true;
			sendThread.run();
		}
	}
	
	private class SendThread extends Thread {
		InetAddress inetAddress = null;
		public SendThread(InetAddress inetAddress) {
			super();
			this.inetAddress = inetAddress;
		}
		
		@Override
		public void run() {
			try {
				DatagramSocket datagramSocket = null;	
				datagramSocket = new DatagramSocket();
				while (isSending) {
					if ((sendBase + windowSize) % LISTSIZE > nextSeqNumber) {
						LFTP_packet packet = packetList.get(nextSeqNumber);
						DatagramPacket datagramPacket = new DatagramPacket(
								packet.tobyte(), packet.tobyte().length, 
								inetAddress, port);
						datagramSocket.send(datagramPacket);
					} else {					
						Thread.sleep(500);
					}
				}
				datagramSocket.close();		
			} catch (Exception e) {
				System.out.println("发送出错");
			}
		}
	}
	
	private class RecieveThread extends Thread {
		DatagramPacket datagramPacket = null;
		
		@SuppressWarnings("resource")
		@Override
		public void run() {
			try {
				DatagramSocket datagramSocket = new DatagramSocket();
				while (true) {
					datagramSocket.receive(datagramPacket);
					byte[] data = datagramPacket.getData();
					LFTP_packet packet = new LFTP_packet(data);
					
					int ack = packet.getAck();		
					
				}
				datagramSocket.close();
			} catch (Exception e) {
				System.out.println("接收出错");
			}
		}
	}
	
	private class FileThread extends Thread {
		File src = new File(path);
		
	    @Override    
	    public void run() {
	    	InputStream is = null;
	    	try {
	    		is = new FileInputStream(src);
	    	
		    	for (int i = 0; ; i++) {
		    		if ((fileNumber+1)%LISTSIZE == sendBase) {
		    			Thread.sleep(500);
		    			i--;
		    		} else {
		    			byte[] car = new byte[READSIZE];
		    			if (is.read(car) != -1) {
		    				LFTP_packet tem = new LFTP_packet(i, 0, 0, 256, car);
		    				String str = new String(tem.tobyte(), HEADSIZE , tem.tobyte().length-HEADSIZE);	                
		    				System.out.println(str);
		    				System.out.println(fileNumber);
		    				System.out.println("SnedBase: " + sendBase); 
		    				packetList.remove(fileNumber);
		    				packetList.add(fileNumber, tem);
		    				fileNumber = (fileNumber+1)%LISTSIZE;
		    			} else {
		    				is.close();
		    				break;
		    			}
			    		    			
		    		}
		    	}
	    	
	    	} catch(Exception e) {	 
	    		e.printStackTrace();
	    	}
	    	
	    }
	    	
	}
}