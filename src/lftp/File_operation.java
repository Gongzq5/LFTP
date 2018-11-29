package lftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class File_operation {

	public static void main(String[] args) throws IOException {
		File src = new File("C:\\Users\\LENOVO\\Desktop\\test.txt");
        //选择流
        InputStream is = new FileInputStream(src);
        //读取操作
        byte[] car = new byte[10];
        int len = 0;//实际读取大小
        //循环读取
        while((len = is.read(car))!= -1){
            //输出 字节数组转成字符串
            String info = new String(car,0,len);
            System.out.println(info);
            
            
        }
        if(is != null){
            is.close();
        }
	
		
	}
}