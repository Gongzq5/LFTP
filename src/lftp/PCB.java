package lftp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * LCB is the LFTP control block, 
 * which has some control information and method to control an
 * existed LFTP link, such as the src IP, port, dst IP, port, as 
 * well as congestion window, flow control window, or something else.
 * 
 * Run in the sender side.
 * 
 * It must be the most important module in this project.
 * Take care of it. 
 * _________________________________________________
 * |           The must-complete mechanism         | 
 * |-----------------------------------------------|
 * |reliable data transport |   ACKs               |
 * |     flow control       |   flow window        |
 * |  congestion control    |   congestion window  |
 * |-----------------------------------------------|
 * 
 * We need 2 threads for sender, they do the things
 * 1. decide which package should be send
 * 2. receive packages 
 *     >> ACKs, and put it into our data structures
 *     >> Other packages, and deal it
 *     
 * --------------------------------------------------------------
 * The variables of the class's instance and it's meaning
 * --------------------------------------------------------------
 * 
 * src_ip:         Source IP address and port
 * dst_ip:         Destiny IP address and port
 * 
 * rec_window:     receive window size
 * con_window:     congestion control window size
 * 
 * baseACK:        most recently no ACKed seq number
 * seqNumber:      current waiting packeting seq number
 * 
 * @author Gongzq5
 * 
 */
public class PCB {
	DatagramSocket socket;
	
	InetAddress dstIP;		// Destiny IP address 
	int port;				// and port
	
	int recWindowSize; 		// Receive window size
	int conWindowSize;		// Congestion window size
	int baseACK;			// Most recently no ACKed package's sequence number
	int seqNumber;			// sequence number
	
	public PCB(String dIP, int port) throws IOException {
		// TODO Auto-generated constructor stub
		socket = new DatagramSocket(port);
		dstIP = InetAddress.getByName("localhost");
		port = 8800;
	}
	
	public PCB(String dIP) throws IOException {
		// TODO Auto-generated constructor stub
		socket = new DatagramSocket(port);
		dstIP = InetAddress.getByName("localhost");
		port = 0;
	}	
	
	public void send(byte[] data) throws IOException {
		DatagramPacket datagramPackage = new DatagramPacket(data, data.length, dstIP, port);
		socket.send(datagramPackage);		
	}
	
	public byte[] recv() throws IOException {
		byte[] data = new byte[1024];
		DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
		socket.receive(datagramPacket);
		System.out.println("Come from" + datagramPacket.getAddress().toString());
		return datagramPacket.getData();
	}
	
	public void closeSocket() {
		socket.close();
	}	
}

