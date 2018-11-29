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
		
		//1、定义服务器地址、端口号、数据
		InetAddress inetAddress = InetAddress.getByName("localhost");
		int port = 8800;
		byte [] data = "用户名： 最帅的；密码： 123".getBytes();
		//2、创建数据报，包含发送的信息
		DatagramPacket datagramPacket = new DatagramPacket(data,data.length,inetAddress,port);
		//3、创建DatagramSocket对象
		DatagramSocket datagramSocket = new DatagramSocket();
		//4、向服务器端发送数据报
		datagramSocket.send(datagramPacket);
		
		//1、创建数据报，用于接收服务器端响应数据，数据保存到字节数组中
		byte [] data2 = new byte[1024];
		DatagramPacket datagramPacket1 =new DatagramPacket(data2 ,data2.length);
		//2、接收服务器响应的数据
		datagramSocket.receive(datagramPacket1);
		//3、读取数据
		String reply = new String(data2,0,datagramPacket1.getLength());
		System.out.println("这里是客户端，服务器端发来的消息：--"+ reply);
		//4、关闭资源
		datagramSocket.close();
	}
}


