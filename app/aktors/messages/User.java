package aktors.messages;

import org.bson.types.ObjectId;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsString;
import play.api.libs.json.JsString$;
import play.api.libs.json.JsValue;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.util.ArrayList;

/**
 * Created by Patrick Robinson on 02.05.15.
 */
public class User {
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return
            ((obj instanceof User) && ((User)obj).id.equals(id))
        ||  ((obj instanceof JsObject) && new ObjectId(((JsObject)obj).$bslash("id").toString()).equals(id));
    }

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

    public static User fromJSON(JsObject user) {
        return new User(
            new ObjectId(user.$bslash("id").toString())
        ,   user.$bslash("name").toString()
        ,   user.$bslash("password") instanceof JsString ? user.$bslash("password").toString() : ""
        );
    }

	public JsObject toJSON() {
		return toJSON(false);
	}

    public JsObject toJSON(boolean withPw) {
	    ArrayList<Tuple2<String, JsValue>> tuplesj = new ArrayList<>();
	    tuplesj.add(Tuple2.apply("id", JsString$.MODULE$.apply(id.toString())));
	    tuplesj.add(Tuple2.apply("name", JsString$.MODULE$.apply(name)));
	    if(withPw) tuplesj.add(Tuple2.apply("password", JsString$.MODULE$.apply(password)));
	    return new JsObject(JavaConversions.asScalaBuffer(tuplesj));
    }
}
