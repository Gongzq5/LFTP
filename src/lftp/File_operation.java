package lftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class File_operation {
	static private String path;
	private static final int READSIZE = 100;
	private static final int HEADSIZE = 10;
	
	public File_operation(String path_) {
		this.path = path_;
		
	}
	static private int fileNumber = 0;
	static private int sendBase = 0;
	static private int SIZE = 10;
	static private List<LFTP_packet> list = new ArrayList<>();
	
	private static class FileThread extends Thread {
		File src = new File(path);
		
	    @Override    
	    public void run() {
	    	InputStream is = null;
	    	try {
	    		is = new FileInputStream(src);
	    	
		    	for (int i = 0; ; i++) {
		    		if ((fileNumber+1)%SIZE == sendBase) {
		    			Thread.sleep(500);
		    			i--;
		    		} else {
		    			byte[] car = new byte[READSIZE];
		    			if (is.read(car) != -1) {
		    				LFTP_packet tem = new LFTP_packet(i, 0, 0, 256, car);
		    				String str = new String(tem.tobyte(), 6 , tem.tobyte().length-6);	                
		    				System.out.println(str);
		    				System.out.println(fileNumber);
		    				System.out.println("SnedBase: " + sendBase); 
		    				list.add(fileNumber, tem);
		    				fileNumber = (fileNumber+1)%SIZE;
		    			} else {
		    				is.close();
		    				break;
		    			}
			    		    			
		    		}
		    	}
	    	
	    	} catch(Exception e) {	 
	    		e.printStackTrace();
	    	}
	    }	    	
	}
	
	private static class SendThread extends Thread {
		@Override    
	    public void run() {
			while(true) {
				sendBase = (sendBase+1)%SIZE;
				char i = 0;
				try {
					i = (char) System.in.read();
					System.in.read();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				System.out.println("Sned: " + sendBase +  "   " + fileNumber); 
			}
		}
	}
	

	public static void main(String[] args) {	
		File_operation file_operation = new File_operation("C:\\Users\\LENOVO\\Desktop\\test.txt");
		
		(new FileThread()).start();
//		System.out.println("Asdfas");
		(new SendThread()).start();
		
		
	}
}
