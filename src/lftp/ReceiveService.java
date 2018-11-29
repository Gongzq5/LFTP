package lftp;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiveService {
	private static final int LISTSIZE = 256;
	private static final int WRITESIZE = 100;  //4086
	private static final int HEADSIZE = 10;
	private List<LFTP_packet> packetList = new ArrayList<LFTP_packet>(LISTSIZE);
//	private SendThread sendThread = null;
//	private RecieveThread recieveThread = null;
	
	private int receiveBase = 0;	
	private boolean isSending = false;
	private int windowSize = 64;
	private int filereadNumber = 0;
	private int filewriteNumber = 0;
	private Map<Integer, LFTP_packet> packet = new HashMap<Integer, LFTP_packet>();
	private int PORT_NUM = 5066;
	
	private String path = null;
//	private int port = 8080;
	private InetAddress inetAddress = null;
	private byte[] receMsgs = new byte[WRITESIZE+HEADSIZE];
	private DatagramSocket datagramSocket = null;
    private DatagramPacket datagramPacket = null;
	
	private class RecieveThread extends Thread {
	
		@Override
		public void run() {
			try {
				datagramSocket = new DatagramSocket(PORT_NUM);
				datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
				while (true) {
					datagramSocket.receive(datagramPacket);
					byte[] data = datagramPacket.getData();
//					LFTP_packet packet = new LFTP_packet(data);
					
					int ack = packet.getAck();		
					
				}
				datagramSocket.close();
			} catch (Exception e) {
				System.out.println("½ÓÊÕ³ö´í");
			}
		}
	}
	
	
	private class FileThread extends Thread {
		File src = new File(path);
		
	    @Override    
	    public void run() {
	    	FileOutputStream out = null;
	    	try {
	    		out = new FileOutputStream(src);    	
		    	while(true) {
		    		if (filereadNumber == receiveBase) {
		    			Thread.sleep(500);
		    		} else {
		    			filereadNumber = (filereadNumber+1)%LISTSIZE;
		    			if (filewriteNumber+1 == packetList.get(filereadNumber).getSerialNumber()) {
		    				out.write(packetList.get(filereadNumber).getData());
		    				filewriteNumber ++;
		    				if (packetList.get(filereadNumber).getAck() == 1)
		    					break;
		    			} else {
		    				packet.put(packetList.get(filereadNumber).getSerialNumber(), packetList.get(filereadNumber));
		    				while (packet.containsKey(filewriteNumber)) {
		    					out.write(packet.get(filereadNumber).getData());		    					
		    					packet.remove(filereadNumber);
		    					filewriteNumber ++;
		    				}
		    			}		    	
			    		    			
		    		}		    		
		    	}
		    	out.close();
	    	
	    	} catch(Exception e) {	 
	    		e.printStackTrace();
	    	}
	    	
	    }
	}
	
	
	
}