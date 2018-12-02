package lftp;

/*
 * 头部  15 byte
 * 序号： n           大小 4 byte， 32bit
 * 是否为最后一个：1/0   0：不是    1：是       大小 1 byte
 * ack号			    0：来自发送方   1：来自接收方 确认     大小 1 byte
 * 接收窗口			大小4byte  剩余空间大小
 * 确认结束			0：没结束     1：结束  大小 1 byte
 * packet长度                        大小 4 byte
 *  
 * 数据 4090 byte
 *  
 * 整个UDP大小  4k byte   4k*8 bit
 * 
 * PS : 加入版本信息 LFTP 0.1
 */


public class LFTP_head {
	private static final int HEADSIZE = 15;
	private byte[] serial_number;
	private byte is_last;
	private byte ack;
	private byte[] receive_window;
	private byte is_final;
	private byte[] length;
	
	public LFTP_head(int number, int islast, int ack, int receive_window, int is_final, int length) {
		this.serial_number = IntToByte(number);
		this.is_last = (byte)islast;
		this.ack = (byte)ack;
		this.receive_window = IntToByte(receive_window);
		this.is_final = (byte)is_final;
		this.length = IntToByte(length);
		
	}
	
	public LFTP_head(byte[] number, byte islast, byte ack, byte[] receive_window, byte isfinal, byte[] length) {
		this.serial_number = number;
		this.is_last = islast;
		this.ack = ack;
		this.receive_window = receive_window;
		this.is_final = isfinal;
		this.length = length;
	}
	
	public int getSerialNumber_int() {
		return Byte2Int(serial_number);
	}
	
	public int getReceiveWindow_int() {
		return Byte2Int(receive_window);
	}
	
	public int getIslast_int() {
		return is_last & 0xFF;
	}
	
	public int getAck_int() {
		return ack & 0xff;
	}
	
	public int getIsfinal_int() {
		return is_final & 0xff;
	}
	
	public int getLength_int() {
		return Byte2Int(length);
	}
	
	public byte[] getSerialNumber_byte() {
		return serial_number;
	}
	
	public byte[] getReceiveWindow_byte() {
		return receive_window;
	}
	
	public byte getIslast_byte() {
		return is_last;
	}
	
	public byte getAck_byte() {
		return ack;
	}
	
	public byte getIsfinal_byte() {
		return is_final;
	}
	
	public byte[] getLength_byte() {
		return length;
	}
	
	public byte[] tobyte() {
		byte[] res = new byte[HEADSIZE];
		System.arraycopy(serial_number, 0, res, 0, 4);
		res[4] = is_last;
		res[5] = ack;
		System.arraycopy(receive_window, 0, res, 6, 4);
		res[10] = is_final;
		System.arraycopy(length, 0, res, 11, 4);
		
		return res;
	}
			
	
	public static int Byte2Int(byte[] bytes) {
		return (bytes[0]&0xff)<<24
			| (bytes[1]&0xff)<<16
			| (bytes[2]&0xff)<<8
			| (bytes[3]&0xff);
	}
	
	public static byte[] IntToByte(int num){
		byte[]bytes=new byte[4];
		bytes[0]=(byte) ((num>>24)&0xff);
		bytes[1]=(byte) ((num>>16)&0xff);
		bytes[2]=(byte) ((num>>8)&0xff);
		bytes[3]=(byte) (num&0xff);
		return bytes;
	}

	
	public static void main(String[] args) {
		LFTP_head test = new LFTP_head(120101, 1, 0, 256, 1, 4081);
		
		System.out.println(test.getSerialNumber_int());
		System.out.println(test.getIslast_int());
		System.out.println(test.getAck_int());
		System.out.println(test.getReceiveWindow_int());
		System.out.println(test.getIsfinal_int());
		System.out.println(test.getLength_int());
		
		System.out.println();
	}
}