package lftp;

/*
 * ͷ��  10 byte
 * ��ţ� n           ��С 4 byte�� 32bit
 * �Ƿ�Ϊ���һ����1/0   0������    1����       ��С 1 byte
 * ack��			    0�����Է��ͷ�   1�����Խ��շ� ȷ��     ��С 1 byte
 * ���մ���			��С4byte  ʣ��ռ��С
 *  
 * ���� 4090 byte
 *  
 * ����UDP��С  4k byte   4k*8 bit
 */


public class LFTP_head {
	private byte[] serial_number;
	private byte is_last;
	private byte ack;
	private byte[] receive_window;
	
	public LFTP_head(int number, int islast, int ack, int receive_window) {
		this.serial_number = IntToByte(number);
		this.is_last = (byte)islast;
		this.ack = (byte)ack;
		this.receive_window = IntToByte(receive_window);
		
	}
	
	public LFTP_head(byte[] number, byte islast, byte ack, byte[] receive_window) {
		this.serial_number = number;
		this.is_last = islast;
		this.ack = ack;
		this.receive_window = receive_window;
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
	
	public byte[] tobyte() {
		byte[] res = new byte[10];
		System.arraycopy(serial_number, 0, res, 0, 4);
		res[4] = is_last;
		res[5] = ack;
		System.arraycopy(receive_window, 0, res, 6, 4);
		
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
		LFTP_head test = new LFTP_head(120101, 1, 0, 256);
		
		System.out.println(test.getSerialNumber_int());
		System.out.println(test.getIslast_int());
		System.out.println(test.getAck_int());
		System.out.println(test.getReceiveWindow_int());
		
//		for (int i = 0; i < test.getSerialNumber_byte().length; i++) {
//			System.out.println(test.getSerialNumber_byte()[i]);
//		}
		System.out.println();
		
//		byte[] test2 = test.tobyte();
//		for (int i = 0; i < test2.length; i++) {
//			System.out.println(test2[i]);
//		}
		
	
	}
}