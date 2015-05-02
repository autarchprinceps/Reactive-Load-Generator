package aktors.messages;

/**
 * Created by Patrick Robinson on 02.05.15.
 */
public class DBGetCMD {
    public enum Type {
        AllPlansForUser, PlanByID, RunByID, UserByID // TODO runRaw?
    }

    public Type t;
    public int id;
}
