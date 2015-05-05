package aktors.messages;

/**
 * Created by Patrick Robinson on 05.05.15.
 */
public class DBDelCMD {
    public enum Type {
        Plan, Run, User
    }

    public Type t;
    public int id;
}
