package lftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class File_operation {

	public static void main(String[] args) throws IOException {
		File src = new File("C:\\Users\\LENOVO\\Desktop\\test.txt");
        //ѡ����
        InputStream is = new FileInputStream(src);
        //��ȡ����
        byte[] car = new byte[10];
        int len = 0;//ʵ�ʶ�ȡ��С
        //ѭ����ȡ
        while((len = is.read(car))!= -1){
            //��� �ֽ�����ת���ַ���
            String info = new String(car,0,len);
            System.out.println(info);
            
            
        }
        if(is != null){
            is.close();
        }
	
		
	}
}