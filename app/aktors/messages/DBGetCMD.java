package aktors.messages;

import org.bson.types.ObjectId;

/**
 * Created by Patrick Robinson on 02.05.15.
 */
public class DBGetCMD {
    @Override
    public int hashCode() {
        return t.hashCode() + id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DBGetCMD && ((DBGetCMD)obj).t.equals(t) && ((DBGetCMD)obj).id.equals(id);
    }

    public enum Type {
        AllPlansForUser, AllRunsForPlan, PlanByID, RunByID, UserByID, RunRaws
    }

	public Type t;
    public ObjectId id;
}
