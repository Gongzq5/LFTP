package lftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SendService {
	private static final int LISTSIZE = 256; //256
	private static final int READSIZE = 4085;  //4085
	private static final int HEADSIZE = 11;
	
	private List<LFTP_packet> packetList = null;
	private SendThread sendThread = null;
	private RecieveThread recieveThread = null;
	private FileThread fileThread = null;
	private Timer timer = null;
	
	private int sendBase = 0;	
	private int fileNumber = 0;
	private int nextSeqNumber = 0;
	private boolean isSending = false;
	private int windowSize = 128; //128
	private long timeOut = 300;
	
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
		unUsedAck = new HashMap<Integer, Boolean>();
//		reSendQueue = new LinkedList<>();
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
//				DatagramSocket datagramSocket = null;	
				datagramSocket = new DatagramSocket();
				while (isSending) {
//					System.out.println("即将发送： " + nextSeqNumber);
					// 先发重传的包
					while (!reSendQueue.isEmpty()) {
						LFTP_packet packet = reSendQueue.poll();
						packet.setTime(System.currentTimeMillis());
						DatagramPacket datagramPacket = new DatagramPacket(
								packet.tobyte(), packet.tobyte().length, 
								inetAddress, port);
						datagramSocket.send(datagramPacket);
//						System.out.println("chongfa" + packet.getSerialNumber());
					}
//					System.out.println("即将发送2： " + nextSeqNumber);
					boolean willSend = false;
					if (nextSeqNumber == fileNumber) {
						willSend = false;
					} else if (sendBase <= nextSeqNumber) {
						willSend = ((sendBase + windowSize) > nextSeqNumber);
					} else {
						willSend = ((sendBase + windowSize) % LISTSIZE > nextSeqNumber);
					} 
					
					
//					System.out.println("即将发送3： " + willSend);
					// 然后发正常的包
					if (willSend) {
//						nextSeqNumber = (nextSeqNumber+1) % LISTSIZE;
						LFTP_packet packet = packetList.get(nextSeqNumber);
						packet.setTime(System.currentTimeMillis());
						DatagramPacket datagramPacket = new DatagramPacket(
								packet.tobyte(), packet.tobyte().length, 
								inetAddress, port);
//						datagramSocket.setReuseAddress(true); 
//						System.out.println("发送");
						datagramSocket.send(datagramPacket);
//						System.out.println(datagramPacket.getAddress() + "  " + datagramPacket.getPort());

						nextSeqNumber = (nextSeqNumber+1) % LISTSIZE;
					} else {					
						Thread.sleep(10);
					}
					System.out.println("已发送：(nextSeqNumber) " + (nextSeqNumber) + " (sendBase): " + sendBase +
							"   "  + " fileNumber: " + fileNumber + "   " + willSend);
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
//				DatagramSocket datagramSocket = new DatagramSocket(port);
//				datagramSocket.setReuseAddress(true); 
//				datagramSocket.bind(new InetSocketAddress(inetAddress, port));
				
//				datagramSocket.bind(new InetSocketAddress(port));
				Thread.sleep(10);
				while (true) {
//					System.out.println("等待接收: "+ sendBase);
					datagramSocket.receive(datagramPacket);
					
//					byte[] data = datagramPacket.getData();
					LFTP_packet packet = new LFTP_packet(receMsgs);
					
					if (packet.getIsfinal() == 1) {
						break;
					}
					
//					if (packet.getAck() == 1) {
//						String string = new String(packet.getData(), 0 , packet.getData().length);
//						System.out.println("接收到了: ？" + string);	
//					}

					// 如果收到来自接收方的ack,那么更新sendBase,否则把他加入到unUsedAck里
					if (packet.getAck() == 1 && packet.getSerialNumber() == packetList.get(sendBase).getSerialNumber()) {
						sendBase = (sendBase+1)%LISTSIZE;
						// sendBase递增，然后查看下一个sendBase是否被确认过
						// 是的话递增，直到一个sendBase没有被确认为止ֹ
						if (!unUsedAck.isEmpty()) {
							while (unUsedAck.containsKey(packetList.get(sendBase).getSerialNumber())) {
								unUsedAck.remove(packetList.get(sendBase).getSerialNumber());
								sendBase = (sendBase+1)%LISTSIZE;
							}
						}
					} else if (packet.getAck() == 1) {
						unUsedAck.put(packet.getSerialNumber(), true);
					}
				}
				isSending = false;
				datagramSocket.close();
//				System.out.println("fsdfad");
			} catch (Exception e) {
				e.printStackTrace();
//				System.out.println("接收出错");
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
//		    			System.out.println("空间已满，等待发送");
		    			i--;
		    		} else {
		    			byte[] car = new byte[READSIZE];
		    			if (is.read(car) != -1) {		    				
		    				LFTP_packet tem = new LFTP_packet(i, 0, 0, 256, 0, car);
		    				if (fileNumber == 0)
		    					System.out.println("读取文件  存到 " + fileNumber + " 此时SendBase： " + sendBase + " 文 件号：" + i);
		    				if (packetList.size() > fileNumber) {
		    					packetList.set(fileNumber, tem);
		    				} else {
		    					packetList.add(fileNumber, tem);
		    				}
		    				fileNumber = (fileNumber+1)%LISTSIZE;
		    			} else {
		    				is.close();
		    				LFTP_packet tem = new LFTP_packet(i, 1, 0, 256, 0, car);
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
			for (int i = sendBase; i < nextSeqNumber; i=(i+1)%LISTSIZE) {
				if (curr - packetList.get(i).getTime() > timeOut) {
					
					reSendQueue.add(packetList.get(i));
				}
			}
		}
	}
	
	public static void main(String[] args) throws UnknownHostException {
		InetAddress inetAddress = InetAddress.getLocalHost();
		SendService test = new SendService(inetAddress, "D:\\a.txt");
		test.send();
	}
}