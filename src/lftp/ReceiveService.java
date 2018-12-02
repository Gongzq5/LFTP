package lftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReceiveService {
	private static final int LISTSIZE = 256;
	private static final int WRITESIZE = 4081;
	private static final int HEADSIZE = 15;
	private static final int TIMEOUT = 5000;
	
	private static final String REFRESH_WINDOW_SIZE_MSG = "refresh window size";
	private static final String FINAL_MSG = "final";
	
	private List<LFTP_packet> packetList = null;

	private RecieveThread recieveThread = null;
	private FileThread fileThread = null;
	
	private int receiveBase = 0;	
	private int windowSize = 200; // 256
	private int filereadNumber = 0;
	private int filewriteNumber = 0;
	private Map<Integer, LFTP_packet> packet = new ConcurrentHashMap<Integer, LFTP_packet>();
	
	private String path = null;
	private byte[] receMsgs = new byte[WRITESIZE+HEADSIZE];
	private DatagramSocket datagramSocket ;
    private DatagramPacket datagramPacket ;
    
    private Map<Integer, Boolean> recievedPackets = new HashMap<>();
	
    private boolean filewriteOver = false;
    private int leftWindowSize = LISTSIZE;
    
	public ReceiveService(int port, String path) {
		packetList = Collections.synchronizedList(new LinkedList<LFTP_packet>());
		this.path = path;

		try {
			datagramSocket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}
	
	public ReceiveService(DatagramSocket datagramSocket, String path) {
		packetList = Collections.synchronizedList(new LinkedList<LFTP_packet>());
		this.datagramSocket = datagramSocket;
		this.path = path;
	}
	

	
	public void receive() throws UnknownHostException, InterruptedException {
		if (fileThread == null) {
			fileThread = new FileThread();
			fileThread.start();
		}

		if (recieveThread == null) {
			recieveThread = new RecieveThread();
			recieveThread.start();
			filewriteOver = false;
		}
		
		fileThread.join();
		recieveThread.join();
	}
	
	private class RecieveThread extends Thread {
		
		@Override
		public void run() {
			try {
				datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
				datagramSocket.setSoTimeout(TIMEOUT);
				while (!filewriteOver) {
					 // 流量控制，如果出现了窗口为0的情况，每隔0.01秒重发一次自己的窗口信息, 此条件判断成功时，跳出其余步骤继续循环
					if (leftWindowSize == 0) {
						sleep(10);
		                leftWindowSize = receiveBase > filereadNumber ? 
								receiveBase - filereadNumber : LISTSIZE - filereadNumber + receiveBase;
	            		LFTP_packet refreshWindowSizePacket = new LFTP_packet(0, 0, 1,
								leftWindowSize, 0, REFRESH_WINDOW_SIZE_MSG.length(), REFRESH_WINDOW_SIZE_MSG.getBytes());
	            		DatagramPacket sendPacket = new DatagramPacket(refreshWindowSizePacket.tobyte(), 
	            				refreshWindowSizePacket.tobyte().length, datagramPacket.getAddress(), 
			            		datagramPacket.getPort());
			            datagramSocket.send(sendPacket);
			            continue;
			        } 
					
					boolean willReceive = false;
					
					if (receiveBase < filereadNumber) {
						willReceive = ((filereadNumber + windowSize) % LISTSIZE > receiveBase);
					} else {
						willReceive = ((filereadNumber + windowSize) > receiveBase);
					}
					if (!willReceive) {
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
							
							leftWindowSize = receiveBase > filereadNumber ? 
									receiveBase - filereadNumber : LISTSIZE - filereadNumber + receiveBase;
							
							LFTP_packet tem2 = new LFTP_packet(tem.getSerialNumber(), 0, 1,
									leftWindowSize, 0, 2, "ok".getBytes());
				            DatagramPacket sendPacket = new DatagramPacket(tem2.tobyte(), 
				            		tem2.tobyte().length, datagramPacket.getAddress(), 
				            		datagramPacket.getPort());
				            datagramSocket.send(sendPacket);
				            
				            System.out.println("接收到了: ？" + tem.getSerialNumber() + " 并且留下来这个" + ", 发回了：" + tem2.getSerialNumber());	
				           
						} catch (InterruptedIOException e) { 
							// 当receive不到信息或者receive时间超过3秒时，就向服务器重发请求
						} 
					}
				} // end while
				
				LFTP_packet final_pac = new LFTP_packet(0, 1, 1, 0, 1, FINAL_MSG.length(), FINAL_MSG.getBytes());
		    	System.out.println("final pac " + final_pac.getIsfinal());
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
		    		
		    		if (filereadNumber == receiveBase) {
		    			System.out.println("还没接受到文件，当前：" + filereadNumber + "  " + receiveBase +" 我要写：" + filewriteNumber);
		    			Thread.sleep(10);
		    		} else {
		    			System.out.println("写文件啦 ，写：" + filewriteNumber + "  接收到的：" + packetList.get(filereadNumber).getSerialNumber() + " is last " + 
		    						packetList.get(filereadNumber).getIslast());
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
		    				filewriteNumber++;

		    			} else {
		    				packet.put(packetList.get(filereadNumber).getSerialNumber(), 
		    						packetList.get(filereadNumber));
		    				boolean trueOver = false;
		    				while (packet.containsKey(filewriteNumber)) {
		    					out.write(packet.get(filewriteNumber).getData());
		    					if (packet.get(filewriteNumber).getIslast() == 1) {
		    						trueOver = true;
		    						break;
		    					}
		    					packet.remove(filewriteNumber);
		    					filewriteNumber++;
		    				}
		    				if (trueOver) break;
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
		
	public static void main(String[] args) throws UnknownHostException, InterruptedException {
		DatagramSocket datagramSocket = null;
		try {
			datagramSocket = new DatagramSocket(5066);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		ReceiveService test = new ReceiveService(datagramSocket, "test\\dst10m.txt");
		test.receive();
	}
}