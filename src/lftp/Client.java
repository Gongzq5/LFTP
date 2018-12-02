package lftp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.cert.TrustAnchor;
import java.util.Scanner;

public class Client {
	private static final String PROMPT = "LFTP > ";
	private static final String GET_COMMAND = "lget";
	private static final String SEND_COMMAND = "lsend";
	private static final String QUIT_COMMAND = "lquit";
	
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
				System.out.println("Input wrong...");
			}
		}
		System.out.println("LFTP Client quits now, thanks for using.");
	}
	
	private void getFile(String _serverIP, String _serverPort, String _filePath) {
		try {
			InetAddress serverAddress = InetAddress.getByName(_serverIP);
			int serverPort = Integer.valueOf(_serverPort);
			String filePath = _filePath;
			
			String packetMsg = "GET" + (byte)filePath.length() + filePath;
			DatagramPacket requestPacket = new DatagramPacket(packetMsg.getBytes(), packetMsg.getBytes().length,
					serverAddress, serverPort);
			DatagramPacket datagramPacket = new DatagramPacket(new byte[1024], 1024);
			
			System.out.println("Request send to the server ( " + _serverIP + ":" + _serverPort + " )");
			System.out.println("Waiting for response...");
			
			datagramSocket.setSoTimeout(5000);
			int timeOutTimes = 0;
			while (timeOutTimes < 10) {
				try {
					datagramSocket.send(requestPacket);
					datagramSocket.receive(datagramPacket);
					
					String receiveMsg = datagramPacket.getData().toString();
					
					String tag = receiveMsg.substring(0, CMD_LEN);
					System.out.println(tag);
					if (tag.equals("ACK")) {
						String addr = receiveMsg.substring(CMD_LEN, CMD_LEN+IP_LEN);
						if (addr.equals(InetAddress.getLocalHost().getAddress().toString())) {
							System.out.println("Data transfer begin, please wait in patient...");
							receiveService = new ReceiveService(datagramSocket, "D:\\RCV.txt");
							receiveService.receive();
							break;
						}
					}
				} catch (Exception e) {
					System.out.println("Response time out, request resent " + ++timeOutTimes + "th times. Waiting for response...");
				}
			}
			System.out.println("Data transfer over, save in D:\\RCV.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void sendFile(String serverIP, String serverPort, String filePath) {
		
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
 */