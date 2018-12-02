import java.util.HashMap;
import java.util.Map;

public class test {

	public test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		Map<Integer, Integer> m = new HashMap<Integer, Integer>() {{
			put(1, 1);
		}};
		System.out.println(m.size() + " " + m.get(1));
		m.put(1, 2);
		System.out.println(m.size() + " " + m.get(1));
	}
}
