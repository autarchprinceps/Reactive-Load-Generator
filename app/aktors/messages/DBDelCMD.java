package aktors.messages;

import org.bson.types.ObjectId;

/**
 * Created by Patrick Robinson on 05.05.15.
 */
public class DBDelCMD {
    @Override
    public int hashCode() {
        return t.hashCode() + id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DBDelCMD && ((DBDelCMD)obj).t.equals(t) && ((DBDelCMD)obj).id.equals(id);
    }

    public enum Type {
        Plan, Run, User
    }

    public Type t;
    public ObjectId id;
}
