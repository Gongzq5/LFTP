package useless;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Client {

	public Client() {}

	public static void main(String[] args) throws IOException {
//		LFTP lftp = new LFTP();
//		byte[] data = "Hello, Server. This is client.".getBytes();
//		lftp.send(data);	
//		lftp.close();
		
		//1�������������ַ���˿ںš�����
		InetAddress inetAddress = InetAddress.getByName("localhost");
		int port = 8800;
		byte [] data = "�û����� ��˧�ģ����룺 123".getBytes();
		//2���������ݱ����������͵���Ϣ
		DatagramPacket datagramPacket = new DatagramPacket(data,data.length,inetAddress,port);
		//3������DatagramSocket����
		DatagramSocket datagramSocket = new DatagramSocket();
		//4����������˷������ݱ�
		datagramSocket.send(datagramPacket);
		
		//1���������ݱ������ڽ��շ���������Ӧ���ݣ����ݱ��浽�ֽ�������
		byte [] data2 = new byte[1024];
		DatagramPacket datagramPacket1 =new DatagramPacket(data2 ,data2.length);
		//2�����շ�������Ӧ������
		datagramSocket.receive(datagramPacket1);
		//3����ȡ����
		String reply = new String(data2,0,datagramPacket1.getLength());
		System.out.println("�����ǿͻ��ˣ��������˷�������Ϣ��--"+ reply);
		//4���ر���Դ
		datagramSocket.close();
	}
}


