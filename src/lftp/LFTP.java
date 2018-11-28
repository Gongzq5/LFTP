package lftp;

import java.io.IOException;

public class LFTP {
	private PCB pcb;
	
	public LFTP(String dIP) throws IOException {
		// TODO Auto-generated constructor stub
		pcb = new PCB(dIP, 0);
	}
	
	public LFTP(String dIP, int port) throws IOException {
		// TODO Auto-generated constructor stub
		pcb = new PCB(dIP, port);
	}
	
	public void clientUI() throws IOException {
		while (true) {
			System.out.println("Waiting for server...");
			byte[] data = pcb.recv();
			System.out.println(data.toString());
		}
	}
	
	
	public void send(byte[] data) throws IOException {
		pcb.send(data);
		System.out.println("LFTP/send\tOutput ok");
	}
	
	public byte[] recv() throws IOException {
		return pcb.recv();		
	}	
	
	public void close() {
		pcb.closeSocket();
	}
}
