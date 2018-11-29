package lftp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendService {
	private static final int LISTSIZE = 256;
	private List<LFTP_packet> packetList = new ArrayList<LFTP_packet>(LISTSIZE);
	private SendThread sendThread = null;
	private RecieveThread recieveThread = null;
	
	private int sendBase = 0;
	
	private int fileNumber = 0;
	private int nextSeqNumber = 0;
	
	private int windowSize = 64;
	
	private boolean isSending = false;
	
	private Map<Integer, Boolean> unUsedAck = null;
	
	public SendService() {
		unUsedAck = new HashMap<Integer, Boolean>();
	}

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
								inetAddress, 8800);
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
				while (sendBase != nextSeqNumber) {
					datagramSocket.receive(datagramPacket);
					byte[] data = datagramPacket.getData();
					LFTP_packet packet = new LFTP_packet(data);
					
					int ack = packet.getAck();
					// 如果收到sendBase的ack,那么更新sendBase,否则把他加入到unUsedAck里
					if (ack == packetList.get(sendBase).getSerialNumber()) {
						sendBase++;
						// sendBase递增，然后查看下一个sendBase是否被确认过
						// 是的话递增，直到一个sendBase没有被确认为止
						while (unUsedAck.get(packetList.get(sendBase).getSerialNumber())) {
							unUsedAck.remove(packetList.get(sendBase).getSerialNumber());
							sendBase++;
						}
					} else {
						unUsedAck.put(ack, true);
					}					
				}
				datagramSocket.close();
			} catch (Exception e) {
				System.out.println("接收出错");
			}
		}
	}
	
	private class readThread extends Thread {
		@Override
		public void run() {
			
		}
	}
}
