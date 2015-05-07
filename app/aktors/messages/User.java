package aktors.messages;

import org.bson.types.ObjectId;

/**
 * Created by Patrick Robinson on 02.05.15.
 * TODO basically no security?!
 */
public class User {
    public ObjectId id;
    public String name;
    private String password;

    public boolean check(String passwordCandidate) {
        return password.equals(passwordCandidate);
    }

    public String getPassword() {
        return password;
    }

    public boolean changePassword(String oldPw, String newPw) {
        if(oldPw.equals(password)) {
            password = newPw;
            return true;
        } else {
            return false;
        }
    }

    public User(ObjectId id, String name, String password) {
        this.id = id;
        this.name = name;
        this.password = password;
    }
}
