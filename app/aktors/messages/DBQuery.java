package aktors.messages;

import java.util.Map;

/**
 * Created by Patrick Robinson on 07.05.2015.
 */
public class DBQuery {
	public enum Type {
		Login
	}

	public Type t;
	public Map<String, String> terms;
	public boolean flag;
	public Object result;
}
