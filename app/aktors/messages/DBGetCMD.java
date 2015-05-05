package aktors.messages;

import org.bson.types.ObjectId;

/**
 * Created by Patrick Robinson on 02.05.15.
 */
public class DBGetCMD {
    public enum Type {
        AllPlansForUser, AllRunsForPlan, PlanByID, RunByID, UserByID, RunRaws
    }

    public Type t;
    public ObjectId id;
}
