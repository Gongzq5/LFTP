package lftp;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ReceiveService {
	private static final int LISTSIZE = 256;
	private static final int WRITESIZE = 100;  //4086
	private static final int HEADSIZE = 10;
	private List<LFTP_packet> packetList = new ArrayList<LFTP_packet>(LISTSIZE);
//	private SendThread sendThread = null;
//	private RecieveThread recieveThread = null;
	
	private int receiveBase = 0;	
	private boolean isSending = false;
	private int windowSize = 64;
	
	private String path = null;
	private int port = 8080;
	private InetAddress inetAddress = null;
	
	
	
	
}