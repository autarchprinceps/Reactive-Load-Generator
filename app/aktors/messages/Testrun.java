package aktors.messages;

import akka.actor.ActorRef;
import org.bson.types.ObjectId;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsString$;
import play.api.libs.json.JsValue;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class Testrun {
    public ObjectId id;
    public Testplan testplan;
    public List<ActorRef> subscribers;

    public static Testrun fromJSON(JsObject run) throws MalformedURLException {
        Testrun result = new Testrun();
        result.id = new ObjectId(run.$bslash("id").toString());
	    if(run.$bslash("testplan") instanceof JsObject) result.testplan = Testplan.fromJSON((JsObject)run.$bslash("testplan"));
        return result;
    }

	public JsObject toJSON() {
		return toJSON(true);
	}

	public JsObject toJSON(boolean withPlan) {
		ArrayList<Tuple2<String, JsValue>> tuplesj = new ArrayList<>();
		tuplesj.add(Tuple2.apply("id", JsString$.MODULE$.apply(id.toString())));
		if(withPlan) tuplesj.add(Tuple2.apply("testplan", testplan.toJSON()));
		return new JsObject(JavaConversions.asScalaBuffer(tuplesj));
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
        return
            ((obj instanceof Testrun) && ((Testrun)obj).id.equals(id))
        ||  ((obj instanceof JsObject) && new ObjectId(((JsObject)obj).$bslash("id").toString()).equals(id));
	}
}
