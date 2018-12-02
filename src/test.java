import java.net.InetAddress;
import java.net.UnknownHostException;

public class test {

	public test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) throws UnknownHostException {
//		Map<Integer, Integer> m = new HashMap<Integer, Integer>() {{
//			put(1, 1);
//		}};
//		System.out.println(m.size() + " " + m.get(1));
//		m.put(1, 2);
//		System.out.println(m.size() + " " + m.get(1));
		InetAddress inetAddress = InetAddress.getLocalHost();
		System.out.println(new String(inetAddress.getAddress()));
//		System.out.println(inetAddress.getAddress());
	}
}
