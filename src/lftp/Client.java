package lftp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
	private static final String PROMPT = "LFTP > ";
	private static final String GET_COMMAND = "lget";
	private static final String SEND_COMMAND = "lsend";
	private static final String QUIT_COMMAND = "lquit";
	
	private static final String GET = "GET";
	private static final String SEND = "SED";
	private static final String ACK = "ACK";
	private static final String HEART = "HRT";
	
	private static final int CMD_LEN = 3;
	
	private InetAddress serverAddress;
	private int serverPort;
	private byte[] hashId = new byte[4];
	
	private Scanner scanner = null;
	private DatagramSocket datagramSocket = null;
	
	private ReceiveService receiveService = null;
	private SendService sendService = null;
	
	private Timer timer = null;
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
	
	public Client() throws SocketException {
		scanner = new Scanner(System.in);
		datagramSocket = new DatagramSocket();
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		if (timer != null) timer.cancel();
		if (datagramSocket != null) datagramSocket.close(); 
		super.finalize();
	}
	
	public void UILoop() {
		String cmd;
		while (true) {
			try {
				// The prompt, such as LFTP >  
				System.out.print(PROMPT);
				// Get a command such as get/send/quit
				cmd = scanner.next();
				if (cmd.equals(QUIT_COMMAND)) {
					break;
				} else {
					// Not quit, then we should get a IP address of the server
					String _serverIP = scanner.next("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
					String _serverPort = scanner.next("[0-9]{1,5}");
					// Cast from String and calculate ID code
					serverAddress = InetAddress.getByName(_serverIP);
					serverPort = Integer.valueOf(_serverPort);
					byte[] addrAddPort = LFTP_head.IntToByte(Byte2Int(InetAddress.getLocalHost().getAddress()) 
							+ datagramSocket.getLocalPort());
					hashId = LFTP_head.IntToByte(addrAddPort.hashCode());
					
					// path
					String filePath = scanner.next();
					if (cmd.equals(GET_COMMAND)) {
						getFile(filePath);
					} else if (cmd.equals(SEND_COMMAND)) {
						sendFile(filePath);
					}
				}
			} catch (Exception e) {
				System.out.println("Cannot resolve input.");
			}
		}
		System.out.println("LFTP Client quits now, thanks for using.");
	}
	
	private void getFile(String filePath) {
		try {		
			byte[] buf = new byte[1024];
			System.arraycopy(GET.getBytes(), 0, buf, 0, GET.getBytes().length); // 'GET'
			System.arraycopy(hashId, 0, buf, 3, 4);
			buf[7] = (byte)filePath.length();									// path长度
			System.arraycopy(filePath.getBytes(), 0, buf, 8, filePath.getBytes().length); // filePath			
			DatagramPacket requestPacket = new DatagramPacket(buf, buf.length,
					serverAddress, serverPort);
			
			DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
			
			System.out.println("Request send to the server ( " + serverAddress.getHostAddress() + 
					":" + serverPort + " )");
			System.out.println("Waiting for response...");
			
			if (datagramSocket == null || datagramSocket.isClosed()) {
				datagramSocket = null;
				datagramSocket = new DatagramSocket();
			}
			
			datagramSocket.setSoTimeout(5000);
			int timeOutTimes = 0;
			while (timeOutTimes < 6) {
				try {
					datagramSocket.send(requestPacket);
					datagramSocket.receive(datagramPacket);
					
					String receiveMsg = new String(datagramPacket.getData());
					
					String tag = receiveMsg.substring(0, CMD_LEN);
					System.out.println("[Client UI] ACK received, " + tag + " from " + datagramPacket.getAddress() + ":" 
										+ datagramPacket.getPort());
					if (tag.equals(ACK)) {
						byte[] addressByte = new byte[4];
						System.arraycopy(datagramPacket.getData(), 3, addressByte, 0, 4);
						System.out.println("Analysis of received ACK");
						if (Arrays.equals(addressByte, hashId)) {
							// 获取 port 重发 GET
							System.out.println("Analysis success");
							
							byte[] portByte = new byte[4];
							System.arraycopy(datagramPacket.getData(), 7, portByte, 0, 4);
							int getport = Byte2Int(portByte);
							
							System.out.println("Get new port: " + getport);
							
							byte[] buf2 = new byte[1024];
							System.arraycopy(GET.getBytes(), 0, buf2, 0, GET.getBytes().length); // 'GET'
							System.arraycopy(hashId, 0, buf2, 3, 4);
							DatagramPacket requestPacket2 = new DatagramPacket(buf, buf.length,
									serverAddress, getport);
							datagramSocket.send(requestPacket2);
							
							System.out.println("send success");				
							DatagramPacket d2 = new DatagramPacket(new byte[1024], 1024, serverAddress ,getport);
							datagramSocket.receive(d2);
							System.out.println("receive success");
							
							
							// 重发后接收 ACK
							String receiveMsg2 = new String(d2.getData());
							String tag2 = receiveMsg2.substring(0, CMD_LEN);
							System.out.println("[Client UI] ACK received second, " + tag2 + " from " + d2.getAddress() + ":" 
												+ d2.getPort());
							
							if (tag2.equals(ACK)) {
								System.out.println("Data transfer begin, please wait in patient...");
								timer = new Timer();
								timer.schedule(new HeartBeat(), 0, 2000);
								receiveService = new ReceiveService(datagramSocket, "test\\RCV.txt");
								System.out.println("receive begin");
								receiveService.receive();
								timer.cancel();
								System.out.println("receive over");
							}					
							
							break;
						}
					}
				} catch (Exception e) {
					System.out.println("Response time out, request resent " + ++timeOutTimes + "th times. Waiting for response...");
				}
			}
			System.out.println("Data transfer over, save in test\\RCV.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendFile(String filePath) {
		try {
			byte[] buf = new byte[1024];
			System.arraycopy(SEND.getBytes(), 0, buf, 0, SEND.getBytes().length);
			System.arraycopy(hashId, 0, buf, 3, 4);
			buf[7] = (byte)filePath.length();
			System.arraycopy(filePath.getBytes(), 0, buf, 8, filePath.getBytes().length);			
			
			DatagramPacket requestPacket = new DatagramPacket(buf, buf.length,
					serverAddress, serverPort);
			DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
			
			if (datagramSocket == null || datagramSocket.isClosed()) {
				datagramSocket = new DatagramSocket();
			}
			datagramSocket.setSoTimeout(3000);
			System.out.println("Send request to the server...");
			
			int timeOutTimes = 0;
			while (timeOutTimes < 5) {
				try {
					datagramSocket.send(requestPacket);
					System.out.println("Waiting for server response...");
					datagramSocket.receive(datagramPacket);
					
					String receiveMsg = new String(datagramPacket.getData());
					
					String tag = receiveMsg.substring(0, CMD_LEN);
					System.out.println("tag " + tag);
					if (tag.equals(ACK)) {
						byte[] addressByte = new byte[4];
						System.arraycopy(datagramPacket.getData(), 3, addressByte, 0, 4);
						
						if (Arrays.equals(addressByte, hashId)) {
							byte[] portByte = new byte[4];
							System.arraycopy(datagramPacket.getData(), 7, portByte, 0, 4);
							int targetPort = Byte2Int(portByte);
							System.out.println("port " + targetPort);
							
							System.out.println("Data transfer begin, please wait in patient...");
							timer = new Timer();
							timer.schedule(new HeartBeat(), 0, 2000);
							sendService = new SendService(serverAddress, targetPort, filePath);
							sendService.send();
							timer.cancel();
							System.out.println("Send over");
							break;
						}
					}
				} catch (Exception e) {
					System.out.println("Waiting timeout, resend the request " + timeOutTimes + " th time.");
					timeOutTimes++;
				}
			}
			System.out.println("File send over, thanks for using lftp.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public class HeartBeat extends TimerTask {
		@Override
		public void run() {
			try {
				// 'HRT'+hashId
				byte[] msg = new byte[7];
				System.arraycopy(HEART.getBytes(), 0, msg, 0, HEART.getBytes().length);
				System.arraycopy(hashId, 0, msg, HEART.getBytes().length, hashId.length);
				datagramSocket.send(new DatagramPacket(msg, msg.length, 
						serverAddress, serverPort));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static int Byte2Int(byte[] bytes) {
		return (bytes[0]&0xff)<<24
			 | (bytes[1]&0xff)<<16
			 | (bytes[2]&0xff)<<8
			 | (bytes[3]&0xff);
	}
	
	public static void main(String[] args) {
		try {
			new Client().UILoop();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	} 
}


/**
 * > get a.txt 
 * 
 * get 请求
 * 分配端口，给我发回来
 *  172.19.1.76
 * lget 172.19.1.76 5066 test\\src10m.txt
 * lsend 172.19.1.76 5066 test\\src10m.txt
 * 
 * lget 172.18.34.154 5066 test\\src10m.txt
 * lget 172.18.35.215 5066 test\\src10m.txt
 * 
 * lsend 172.18.34.154 5066 test\\src10m.txt
 * 
 * lsend 120.77.155.38 5066 test\\src10m.txt
 * lget 120.77.155.38 5066 test\\src10m.txt
 */