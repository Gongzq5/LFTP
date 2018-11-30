package lftp;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiveService {
	private static final int LISTSIZE = 10; //256
	private static final int WRITESIZE = 100;  //4086
	private static final int HEADSIZE = 10;
	private List<LFTP_packet> packetList = new ArrayList<LFTP_packet>(LISTSIZE);

	private RecieveThread recieveThread = null;
	private FileThread fileThread = null;
	
	private int receiveBase = 0;	
	private int windowSize = 5; //64
	private int filereadNumber = 0;
	private int filewriteNumber = 0;
	private Map<Integer, LFTP_packet> packet = new HashMap<Integer, LFTP_packet>();
	private int PORT_NUM = 5066;
	
	private String path = null;
	private byte[] receMsgs = new byte[WRITESIZE+HEADSIZE];

	
	public ReceiveService(int port, String path) {
		PORT_NUM = port;
		this.path = path;
	}
	
	public void receive() throws UnknownHostException {
		if (fileThread == null) {
			fileThread = new FileThread();
			fileThread.start();
		}

		if (recieveThread == null) {
			recieveThread = new RecieveThread();
			recieveThread.start();
		}
	}
	
	private class RecieveThread extends Thread {
		private DatagramSocket datagramSocket = null;
	    private DatagramPacket datagramPacket = null;
		@Override
		public void run() {
			try {
				datagramSocket = new DatagramSocket(PORT_NUM);
				datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
				while (true) {
					if ((filereadNumber + windowSize)%LISTSIZE  <= receiveBase) {
						Thread.sleep(500);
					} else {				
						System.out.println("等待接收");
						datagramSocket.receive(datagramPacket);
						LFTP_packet tem = new LFTP_packet(receMsgs);
											
						String string = new String(tem.getData(), 0 , tem.getData().length);
						System.out.println("接收到了: ？" + string);	
						
						if (packetList.size() > receiveBase)
	    					packetList.remove(receiveBase);
						packetList.add(receiveBase, tem);
						receiveBase = (receiveBase+1)%LISTSIZE;
						
						System.out.println("接收后：" + receiveBase);
						
						LFTP_packet tem2 = new LFTP_packet(tem.getSerialNumber(), 0, 1,
								windowSize, "ok".getBytes());
			            DatagramPacket sendPacket = new DatagramPacket(tem2.tobyte(), 
			            		tem2.tobyte().length, datagramPacket.getAddress(), 
			            		datagramPacket.getPort());
			            datagramSocket.send(sendPacket);

						if (tem.getIslast() == 1)
							break;						
						
					}
				}
				datagramSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("接收出错");
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
		    			System.out.println("还没接受到文件，当前：" + filereadNumber + "  " + receiveBase);
		    			Thread.sleep(500);
		    		} else {
		    			System.out.println("写文件啦 ，写：" + filewriteNumber + "  接收到的：" + packetList.get(filereadNumber).getSerialNumber());
		    			if (filewriteNumber == packetList.get(filereadNumber).getSerialNumber()) {
		    				out.write(packetList.get(filereadNumber).getData());
		    				filewriteNumber ++;
		    				if (packetList.get(filereadNumber).getIslast() == 1)
		    					break;
		    			} else {
		    				packet.put(packetList.get(filereadNumber).getSerialNumber(), 
		    						packetList.get(filereadNumber));
		    				while (packet.containsKey(filewriteNumber)) {
		    					out.write(packet.get(filewriteNumber).getData());		    					
		    					packet.remove(filewriteNumber);
		    					filewriteNumber ++;
		    				}
		    			}	
		    			filereadNumber = (filereadNumber+1)%LISTSIZE;
			    		    			
		    		}		    		
		    	}
		    	out.close();
	    	
	    	} catch(Exception e) {	 
	    		e.printStackTrace();
	    	}	    	
	    }
	}
		
	public static void main(String[] args) throws UnknownHostException {
		ReceiveService test = new ReceiveService(5066, "C:\\Users\\LENOVO\\Desktop\\receive.txt");
		test.receive();
	}
}