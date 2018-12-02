package lftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


public class SendService {
	private static final int LISTSIZE = 256;
	private static final int READSIZE = 4081;
	private static final int HEADSIZE = 15;
	
	private static final String FINAL_MSG = "final";
	
	private List<LFTP_packet> packetList = null;
	private SendThread sendThread = null;
	private RecieveThread recieveThread = null;
	private FileThread fileThread = null;
	private Timer timer = null;
	
	private int sendBase = 0;	
	private int fileNumber = 0;
	private int nextSeqNumber = 0;
	private boolean isSending = false;
	private int windowSize = 128;
	private long timeOut = 100;
	private int recvWindow = 128;
	
	private String path = null;
	private int port = 5066;
	private InetAddress inetAddress = null;
	
	private Map<Integer, Boolean> unUsedAck = null;
	private Queue<LFTP_packet> reSendQueue = null;
	private DatagramSocket datagramSocket;	
	
	public SendService(InetAddress inetAddress, String path) {
		this.inetAddress = inetAddress;
		this.path = path;
		
		packetList = Collections.synchronizedList(new LinkedList<LFTP_packet>());
		unUsedAck = new ConcurrentHashMap<Integer, Boolean>();
		reSendQueue = new ConcurrentLinkedQueue<>();
	}
	
	public void send() throws UnknownHostException {
		timer = new Timer();
		timer.schedule(new TimeCounter(), 0, 10);
		if (fileThread == null) {
			fileThread = new FileThread();
			fileThread.start();
		}
		if (sendThread == null) {
			sendThread = new SendThread(inetAddress);
			isSending = true;
			sendThread.start();
		}
		if (recieveThread == null) {
			recieveThread = new RecieveThread();
			recieveThread.start();
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
				datagramSocket = new DatagramSocket();
				while (isSending) {
					// 先发重传的包
					while (!reSendQueue.isEmpty()) {
						LFTP_packet packet = reSendQueue.poll();
						packet.setTime(System.currentTimeMillis());
						DatagramPacket datagramPacket = new DatagramPacket(
								packet.tobyte(), packet.tobyte().length, 
								inetAddress, port);
						datagramSocket.send(datagramPacket);
						System.out.println("重发    " + packet.getSerialNumber() + "时间  " + packet.getTime());
					}
					
					boolean willSend = false;
					if (nextSeqNumber == fileNumber) {
						willSend = false;
					} else if (sendBase <= nextSeqNumber) {
						willSend = ((sendBase + windowSize) > nextSeqNumber);
					} else {
						willSend = ((sendBase + windowSize) % LISTSIZE > nextSeqNumber
								&& sendBase + windowSize >= LISTSIZE);
					}
					
					// 然后发正常的包
					if (willSend) {
						LFTP_packet packet = packetList.get(nextSeqNumber);
						packet.setTime(System.currentTimeMillis());
						DatagramPacket datagramPacket = new DatagramPacket(
								packet.tobyte(), packet.tobyte().length, 
								inetAddress, port);
						System.out.println("发送包长度： " + packet.tobyte().length + "  " + packet.getLength());
						datagramSocket.send(datagramPacket);
						
						nextSeqNumber = (nextSeqNumber+1) % LISTSIZE;
					} else {					
						Thread.sleep(10);
					}
				}
				timer.cancel();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("发送出错");
			}
		}
	}
	
	private class RecieveThread extends Thread {
		private byte[] receMsgs = new byte[READSIZE+HEADSIZE];
		DatagramPacket datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);;
		
		@Override
		public void run() {
			try {
				sleep(10);
				while (true) {
					datagramSocket.receive(datagramPacket);
					LFTP_packet packet = new LFTP_packet(receMsgs);
					
					if (packet.getIsfinal() == 1) {
						break;
					}
					
					if (packet.getAck() == 1) {
						System.out.println("接收到了:  " + packet.getSerialNumber());
					}
					
					boolean ackInWindow = false;
					
					if (packet.getAck() == 1) {
						recvWindow = packet.getReceiveWindow();
						if ( packet.getSerialNumber() >= packetList.get(sendBase).getSerialNumber() 
							&& packet.getSerialNumber() <= packetList.get(sendBase).getSerialNumber()+windowSize) {
							ackInWindow = true;
						}
					}
					
					// 如果收到来自接收方的ACK,那么更新sendBase,否则把他加入到unUsedAck里
					if (ackInWindow && packet.getSerialNumber() == packetList.get(sendBase).getSerialNumber()) {
						sendBase = (sendBase+1)%LISTSIZE;
						// sendBase递增，然后查看下一个sendBase是否被确认过
						// 是的话递增，直到一个sendBase没有被确认为止
						while (packetList.size() > sendBase && unUsedAck.containsKey(packetList.get(sendBase).getSerialNumber())) {
							unUsedAck.remove(packetList.get(sendBase).getSerialNumber());
							sendBase = (sendBase+1)%LISTSIZE;
						}
					} else if (packet.getAck() == 1) {
						unUsedAck.put(packet.getSerialNumber(), true);
					}
				}
				
				isSending = false;
				LFTP_packet finalAckPacket = new LFTP_packet(0, 1, 1, 0, 1, FINAL_MSG.length(), FINAL_MSG.getBytes());
				DatagramPacket finalAckDatagramPacket = new DatagramPacket(finalAckPacket.tobyte(), 
						finalAckPacket.tobyte().length, inetAddress, port);
				datagramSocket.setSoTimeout(5000);
				datagramSocket.send(finalAckDatagramPacket);
				
				System.out.println("final");
				while(true) {				
					try {
						datagramSocket.receive(datagramPacket);
						if (new LFTP_packet(datagramPacket.getData()).getIsfinal() == 1) {
							datagramSocket.send(finalAckDatagramPacket);
						}
					} catch (Exception e) {
						break;
					}
				}
				datagramSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
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
		    			Thread.sleep(10);
		    			i--;
		    		} else {
		    			byte[] car = new byte[READSIZE];
		    			int readsize = 0;
		    			if ((readsize = is.read(car)) != -1) {	
		    				LFTP_packet tem ;
		    				if (readsize < READSIZE) {
		    					byte[] removeZero = new byte[readsize];;
		    					System.arraycopy(car, 0, removeZero, 0, readsize);
		    					tem = new LFTP_packet(i, 0, 0, 256, 0, readsize, removeZero);
		    				} else {
		    					tem = new LFTP_packet(i, 0, 0, 256, 0, READSIZE, car);
		    				}
		    				
		    				if (packetList.size() > fileNumber) {
		    					packetList.set(fileNumber, tem);
		    				} else {
		    					packetList.add(fileNumber, tem);
		    				}
		    				fileNumber = (fileNumber+1)%LISTSIZE;
		    			} else {
		    				is.close();
		    				byte[] empty = new byte[0];
		    				LFTP_packet tem = new LFTP_packet(i, 1, 0, 256, 0, 0, empty);
		    				if (packetList.size() > fileNumber) {
		    					packetList.set(fileNumber, tem);
		    				} else {
		    					packetList.add(fileNumber, tem);
		    				}
		    				fileNumber = (fileNumber+1)%LISTSIZE;
		    				break;
		    			}
		    		}
		    	}
	    	} catch(Exception e) {	 
	    		e.printStackTrace();
	    	}
	    }
	}
	
	private class TimeCounter extends TimerTask {
		@Override
		public void run() {
			long curr = System.currentTimeMillis();
			if (sendBase > nextSeqNumber) {
				for (int i = sendBase; i < LISTSIZE; i++) {
					LFTP_packet packet = packetList.get(i);
					if (curr - packet.getTime() > timeOut && !unUsedAck.containsKey(packet.getSerialNumber())) {	
						reSendQueue.add(packet);
					}
				}
				for (int i = 0; i < nextSeqNumber; i++) {
					LFTP_packet packet = packetList.get(i);
					if (curr - packet.getTime() > timeOut && !unUsedAck.containsKey(packet.getSerialNumber())) {	
						reSendQueue.add(packet);
					}
				}
			} else {
				for (int i = sendBase; i < nextSeqNumber; i=(i+1)%LISTSIZE) {
					LFTP_packet packet = packetList.get(i);
					if (curr - packet.getTime() > timeOut && !unUsedAck.containsKey(packet.getSerialNumber())) {	
						reSendQueue.add(packet);
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws UnknownHostException {
		InetAddress inetAddress = InetAddress.getLocalHost();
		SendService test = new SendService(inetAddress, "test\\src10m.txt");
		test.send();
	}
}