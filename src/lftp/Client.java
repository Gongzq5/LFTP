package lftp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	private static final String PROMPT = "LFTP > ";
	private static final String GET_COMMAND = "lget";
	private static final String SEND_COMMAND = "lsend";
	private static final String QUIT_COMMAND = "lquit";
	
	private static final String GET = "GET";
	private static final String SEND = "SED";
	private static final String ACK = "ACK";
	
	private static final int CMD_LEN = 3;
	private static final int IP_LEN = 4;
	
	private Scanner scanner = null;
	private DatagramSocket datagramSocket = null;
	
	private ReceiveService receiveService = null;
	private SendService sendService = null;
	
	public Client() throws SocketException {
		scanner = new Scanner(System.in);
		datagramSocket = new DatagramSocket();
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
					String serverIP = scanner.next("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
					String serverPort = scanner.next("[0-9]{1,5}");
					// path
					String filePath = scanner.next();
					if (cmd.equals(GET_COMMAND)) {
						getFile(serverIP, serverPort, filePath);
					} else if (cmd.equals(SEND_COMMAND)) {
						sendFile(serverIP, serverPort, filePath);
					}
				}
			} catch (Exception e) {
				System.out.println("Cannot resolve input.");
			}
		}
		System.out.println("LFTP Client quits now, thanks for using.");
	}
	
	private void getFile(String _serverIP, String _serverPort, String _filePath) {
		try {
			InetAddress serverAddress = InetAddress.getByName(_serverIP);
			int serverPort = Integer.valueOf(_serverPort);
			String filePath = _filePath;
		
			byte[] buf = new byte[1024];
			System.arraycopy(GET.getBytes(), 0, buf, 0, GET.getBytes().length);
			buf[3] = (byte)filePath.length();
			System.arraycopy(filePath.getBytes(), 0, buf, 4, filePath.getBytes().length);			

			DatagramPacket requestPacket = new DatagramPacket(buf, buf.length,
					serverAddress, serverPort);
			
			DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
			
			System.out.println("Request send to the server ( " + _serverIP + ":" + _serverPort + " )");
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
					System.out.println("tag " + tag);
					if (tag.equals(ACK)) {
						byte[] addressByte = new byte[4];
						System.arraycopy(datagramPacket.getData(), 3, addressByte, 0, 4);
						String addr = Arrays.toString(addressByte);
						System.out.println("address " + addr);
						if (addr.equals(Arrays.toString(InetAddress.getLocalHost().getAddress()))) {
							System.out.println("Data transfer begin, please wait in patient...");
							receiveService = new ReceiveService(datagramSocket, "test\\RCV.txt");
							receiveService.receive();
							
							System.out.println("receive over");
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
	
	private void sendFile(String _serverIP, String _serverPort, String filePath) {
		try {
			InetAddress serverAddress = InetAddress.getByName(_serverIP);
			int serverPort = Integer.valueOf(_serverPort);
			
			byte[] buf = new byte[1024];
			System.arraycopy(SEND.getBytes(), 0, buf, 0, SEND.getBytes().length);
			buf[3] = (byte)filePath.length();
			System.arraycopy(filePath.getBytes(), 0, buf, 4, filePath.getBytes().length);			

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
						String addr = Arrays.toString(addressByte);
						if (addr.equals(Arrays.toString(InetAddress.getLocalHost().getAddress()))) {
							byte[] portByte = new byte[4];
							System.arraycopy(datagramPacket.getData(), 7, portByte, 0, 4);
							int targetPort = Byte2Int(portByte);
							System.out.println("port " + targetPort);
							
							System.out.println("Data transfer begin, please wait in patient...");
							sendService = new SendService(serverAddress, targetPort, filePath);
							sendService.send();
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
 * 
 * lget 172.18.34.154 5066 test\\src10m.txt
 * lget 172.18.35.215 5066 test\\src10m.txt
 * 
 * lsend 172.18.34.154 5066 test\\src10m.txt
 * 
 * lsend 120.77.155.38 5066 test\\src10m.txt
 * lget 120.77.155.38 5066 test\\src10m.txt
 */