package lftp;

public class LFTP_packet {
	private static final int HEADSIZE = 11;
	private LFTP_head head;
	private byte[] packet;
	private State state;
	private long time;
		
	public static enum State {SR, SNR, NS};
	
	
	public LFTP_packet(byte[] packet) {
		byte[] number = new byte[4];
		byte[] number2 = new byte[4];
		System.arraycopy(packet, 0, number, 0, 4);
		System.arraycopy(packet, 6, number2, 0, 4);
		head = new LFTP_head(number, packet[4], packet[5], number2, packet[10]);

		byte[] pac = new byte[packet.length-6];
		System.arraycopy(packet, HEADSIZE, pac, 0, packet.length-HEADSIZE);
		this.packet = pac;
	}
	
	public LFTP_packet(int number, int islast, int ack, int receive_widow, int isfinal, byte[] packet) {
		LFTP_head tem = new LFTP_head(number, islast, ack, receive_widow, isfinal);
		this.head = tem;
		this.packet = packet;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	public void setTime(Long time) {
		this.time = time;
	}
	
	public State getState() {
		return state;
	}
	
	public Long getTime() {
		return time;
	}
	
	public int getSerialNumber() {
		return head.getSerialNumber_int();
	}
	
	public int getIslast() {
		return head.getIslast_int();
	}
	
	public int getIsfinal() {
		return head.getIsfinal_int();
	}
	
	public int getAck() {
		return head.getAck_int();
	}
	
	public int getReceiveWindow() {
		return head.getReceiveWindow_int();
	}
	
	public LFTP_head getHead() {
		return head;
	}
	
	public byte[] getData() {
		return packet;
	}
	
	
	public byte[] tobyte() {
		byte[] res = new byte[HEADSIZE+packet.length];
		System.arraycopy(head.tobyte(), 0, res, 0, HEADSIZE);
		System.arraycopy(packet, 0, res, HEADSIZE, packet.length);
		
		return res;
	}
	
	
	public static void main(String[] args) {
		LFTP_packet test = new LFTP_packet(10, 0, 0, 120, 1, "test".getBytes());
		
		System.out.println(test.getSerialNumber());
		System.out.println(test.getIslast());
		System.out.println(test.getAck());
		System.out.println(test.getReceiveWindow());
		System.out.println(test.getIsfinal());
		String string = new String(test.getData(), 0, test.getData().length);
		System.out.println(string);
		System.out.println(test.tobyte().length);
		
		System.out.println();
		
		LFTP_packet test2 = new LFTP_packet(test.tobyte());
		System.out.println(test2.getSerialNumber());
		System.out.println(test2.getIslast());
		System.out.println(test2.getAck());
		System.out.println(test2.getReceiveWindow());
		System.out.println(test2.getIsfinal());
		String string2 = new String(test2.getData(), 0, test2.getData().length);
		System.out.println(string2);
		System.out.println(test2.tobyte().length);
	
	}
}