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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class SendService {
	private static final int LISTSIZE = 10; //256
	private static final int READSIZE = 100;  //4086
	private static final int HEADSIZE = 10;
	
	private List<LFTP_packet> packetList = new ArrayList<LFTP_packet>(LISTSIZE);
	private SendThread sendThread = null;
	private RecieveThread recieveThread = null;
	private FileThread fileThread = null;
	
	private int sendBase = 0;	
	private int fileNumber = 0;
	private int nextSeqNumber = 0;
	private boolean isSending = false;
	private int windowSize = 5; //64
	private long timeOut = 500;
	
	private String path = null;
	private int port = 5066;
	private InetAddress inetAddress = null;
	
	private Map<Integer, Boolean> unUsedAck = null;
	private Queue<LFTP_packet> reSendQueue = null;
	
	public SendService(InetAddress inetAddress, String path) {
		this.inetAddress = inetAddress;
		this.path = path;
		
		unUsedAck = new HashMap<Integer, Boolean>();
		reSendQueue = new LinkedList<>();
	}
	
	public void send() throws UnknownHostException {
		Timer timer = new Timer();
		timer.schedule(new TimeCounter(), 10);
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
				DatagramSocket datagramSocket = null;	
				datagramSocket = new DatagramSocket();
				while (isSending) {
//					System.out.println("�������ͣ� " + nextSeqNumber);
					// �ȷ��ش��İ�
					while (!reSendQueue.isEmpty()) {
						LFTP_packet packet = reSendQueue.poll();
						packet.setTime(System.currentTimeMillis());
						DatagramPacket datagramPacket = new DatagramPacket(
								packet.tobyte(), packet.tobyte().length, 
								inetAddress, port);
						datagramSocket.send(datagramPacket);
					}
					// Ȼ�������İ�
					if ((sendBase + windowSize) % LISTSIZE > nextSeqNumber) {
//						nextSeqNumber = (nextSeqNumber+1) % LISTSIZE;
						LFTP_packet packet = packetList.get(nextSeqNumber);
						packet.setTime(System.currentTimeMillis());
						DatagramPacket datagramPacket = new DatagramPacket(
								packet.tobyte(), packet.tobyte().length, 
								inetAddress, port);
						datagramSocket.send(datagramPacket);
						nextSeqNumber = (nextSeqNumber+1) % LISTSIZE;
					} else {					
						Thread.sleep(500);
					}
					System.out.println("�ѷ��ͣ� " + (nextSeqNumber-1));
				}
				datagramSocket.close();		
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("���ͳ���");
			}
		}
	}
	
	private class RecieveThread extends Thread {
		private byte[] receMsgs = new byte[READSIZE+HEADSIZE];
		DatagramPacket datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);;
		
		@Override
		public void run() {
			try {
				DatagramSocket datagramSocket = new DatagramSocket();
//				datagramSocket.bind(new InetSocketAddress(inetAddress, port));
//				datagramSocket.setReuseAddress(true); 
//				datagramSocket.bind(new InetSocketAddress(port));
				Thread.sleep(10);
				while (sendBase != nextSeqNumber) {
					System.out.println("�ȴ�����: "+ sendBase);
					datagramSocket.receive(datagramPacket);
					
//					byte[] data = datagramPacket.getData();
					LFTP_packet packet = new LFTP_packet(receMsgs);
										
					String string = new String(packet.getData(), 0 , packet.getData().length);
					System.out.println("���յ���: ��" + string);				

					// ����յ����Խ��շ���ack,��ô����sendBase,����������뵽unUsedAck��
					if (packet.getAck() == 1 && packet.getSerialNumber() == packetList.get(sendBase).getSerialNumber()) {
						sendBase++;
						// sendBase������Ȼ��鿴��һ��sendBase�Ƿ�ȷ�Ϲ�
						// �ǵĻ�������ֱ��һ��sendBaseû�б�ȷ��Ϊֹ
						while (unUsedAck.get(packetList.get(sendBase).getSerialNumber())) {
							unUsedAck.remove(packetList.get(sendBase).getSerialNumber());
							sendBase++;
						}
					} else {
						unUsedAck.put(packet.getSerialNumber(), true);
					}
				}
				isSending = false;
				datagramSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("���ճ���");
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
		    			System.out.println("�ռ��������ȴ�����");
		    			i--;
		    		} else {
		    			byte[] car = new byte[READSIZE];
		    			if (is.read(car) != -1) {		    				
		    				LFTP_packet tem = new LFTP_packet(i, 0, 0, 256, car);
		    				System.out.println("��ȡ�ļ�  �浽 " + fileNumber + " ��ʱSendBase�� " + sendBase + " �� ���ţ�" + i);
		    				if (packetList.size() > fileNumber)
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
		SendService test = new SendService(inetAddress, "C:\\Users\\LENOVO\\Desktop\\send.txt");
		test.send();
	}
}