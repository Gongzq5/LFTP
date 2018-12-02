package lftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatCodePointException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class ReceiveService {
	private static final int LISTSIZE = 256; //256
	private static final int WRITESIZE = 4081;  //4081
	private static final int HEADSIZE = 15;
	private static final int TIMEOUT = 5000;
	private List<LFTP_packet> packetList = null;

	private RecieveThread recieveThread = null;
	private FileThread fileThread = null;
	
	private int receiveBase = 0;	
	private int windowSize = 128; //128
	private int filereadNumber = 0;
	private int filewriteNumber = 0;
	private Map<Integer, LFTP_packet> packet = new ConcurrentHashMap<Integer, LFTP_packet>();
	private int PORT_NUM = 5066;
	
	private String path = null;
	private byte[] receMsgs = new byte[WRITESIZE+HEADSIZE];
	private DatagramSocket datagramSocket ;
    private DatagramPacket datagramPacket ;
    
    private Map<Integer, Boolean> recievedPackets = new HashMap<>();
	
    private boolean filewriteOver = false;
    
	public ReceiveService(int port, String path) {
		packetList = Collections.synchronizedList(new LinkedList<LFTP_packet>());
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
			filewriteOver = false;
		}
	}
	
	private class RecieveThread extends Thread {
		
		@Override
		public void run() {
			try {
				datagramSocket = new DatagramSocket(PORT_NUM);
				datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
				datagramSocket.setSoTimeout(TIMEOUT);
				while (!filewriteOver) {
					boolean willReceive = false;
					
					if (receiveBase < filereadNumber) {
						willReceive = ((filereadNumber + windowSize) % LISTSIZE > receiveBase);
					} else {
						willReceive = ((filereadNumber + windowSize) > receiveBase);
					}
					System.out.println("recieve base " + receiveBase + "  file read number " + filereadNumber + 
							" will receive " + willReceive);
					if (!willReceive) {
//						System.out.println("sleep");
						Thread.sleep(3);
					} else {				
						try {
							datagramSocket.receive(datagramPacket);
							LFTP_packet tem = new LFTP_packet(datagramPacket.getData());
							
							if (!recievedPackets.containsKey(tem.getSerialNumber())) {
								recievedPackets.put(tem.getSerialNumber(), true);
							}
							
							if (packetList.size() > receiveBase)
								packetList.set(receiveBase, tem);
							else {
								packetList.add(receiveBase, tem);
							}
							receiveBase = (receiveBase+1)%LISTSIZE;
							
							System.out.println("接收后：" + receiveBase);
	
							LFTP_packet tem2 = new LFTP_packet(tem.getSerialNumber(), 0, 1,
									windowSize, 0, 2, "ok".getBytes());
				            DatagramPacket sendPacket = new DatagramPacket(tem2.tobyte(), 
				            		tem2.tobyte().length, datagramPacket.getAddress(), 
				            		datagramPacket.getPort());
				            datagramSocket.send(sendPacket);
				            System.out.println("接收到了: ？" + tem.getSerialNumber() + " 并且留下来这个" + ", 发回了：" + tem2.getSerialNumber());	
						} catch (InterruptedIOException e) { // 当receive不到信息或者receive时间超过3秒时，就向服务器重发请求    
			            	System.out.println("Timed out : " + TIMEOUT );   
			            	System.out.println(packet.size());
			            	System.out.println(packet.size());
			            	System.out.println(packet.size());
			            	System.out.println(packet.size());
						} 
					}
				}
				
				LFTP_packet final_pac = new LFTP_packet(0, 1, 1, 0, 1, 5, "final".getBytes());
		    	DatagramPacket sendPacket = new DatagramPacket(final_pac.tobyte(), 
		    			final_pac.tobyte().length, datagramPacket.getAddress(), 
	            		datagramPacket.getPort());
		    	
		    	while (true) {
			    	datagramSocket.send(sendPacket);
			    	try {
			    		datagramSocket.receive(datagramPacket);
			    		if ((new LFTP_packet(datagramPacket.getData())).getIsfinal() == 1) {
			    			break;
			    		}
					} catch (Exception e) {
						System.out.println("final not find, resend final packet.");
						continue;
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
		    		if (filewriteNumber >= 3652) {
		    			System.out.println("IS LAST " + packetList.get(filereadNumber).getIslast());
		    		}
		    		if (filereadNumber == receiveBase) {
		    			System.out.println("还没接受到文件，当前：" + filereadNumber + "  " + receiveBase +" 我要写：" + filewriteNumber);
		    			Thread.sleep(10);
		    		} else {
		    			System.out.println("写文件啦 ，写：" + filewriteNumber + "  接收到的：" + packetList.get(filereadNumber).getSerialNumber());
		    			if (filewriteNumber == packetList.get(filereadNumber).getSerialNumber()) {
		    				if (packetList.get(filereadNumber).getIslast() == 1) {
		    					break;
		    				}
		    				if (packetList.get(filereadNumber).getLength() != WRITESIZE) {
		    					byte[] tem = new byte[packetList.get(filereadNumber).getLength()];
		    					System.arraycopy(packetList.get(filereadNumber).getData(), 0, tem, 0, packetList.get(filereadNumber).getLength());
		    					out.write(tem);
		    					System.out.println("写文件长度： " +  tem.length + "  " + packetList.get(filereadNumber).getLength());
		    				} else {
		    					out.write(packetList.get(filereadNumber).getData());
		    				}
//		    				System.out.println(packetList.get(filereadNumber).getData().length);
		    				filewriteNumber++;

		    			} else {
		    				packet.put(packetList.get(filereadNumber).getSerialNumber(), 
		    						packetList.get(filereadNumber));
		    				while (packet.containsKey(filewriteNumber)) {
		    					out.write(packet.get(filewriteNumber).getData());		    					
		    					packet.remove(filewriteNumber);
		    					filewriteNumber++;
		    				}
		    			}	
		    			filereadNumber = (filereadNumber+1)%LISTSIZE;
		    		}		    		
		    	}
		    	out.close();
		    	System.out.println("file close"); 
		    	filewriteOver = true;
	    	} catch(Exception e) {	 
	    		e.printStackTrace();
	    	}	    	
	    }
	}
		
	public static void main(String[] args) throws UnknownHostException {
		ReceiveService test = new ReceiveService(5066, "test\\dst10m.txt");
		test.receive();
	}
}