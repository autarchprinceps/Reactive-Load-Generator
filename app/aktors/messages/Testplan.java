package aktors.messages;

import helper.JSONHelper;
import org.bson.types.ObjectId;
import play.api.libs.json.*;
import scala.Tuple2;
import scala.collection.JavaConversions;
import scala.math.BigDecimal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class Testplan {
	@Override
	public int hashCode() {
		return id.hashCode();
	}

    @Override
    public boolean equals(Object obj) {
        return
            ((obj instanceof Testplan) && ((Testplan)obj).id.equals(id))
        ||  ((obj instanceof JsObject) && new ObjectId(((JsObject)obj).$bslash("id").toString()).equals(id));
    }

    public enum ConnectionType {
        HTTP, TCP, UDP, WebSocket
    }

    public ObjectId id;
    public int numRuns; // Per Parallel Worker
    public int parallelity;
    public URL path;
    public int waitBetweenMsgs = 0;
    public int waitBeforeStart = 0;
    public ConnectionType connectionType = ConnectionType.HTTP;
    public User user;

    public static Testplan fromJSON(JsObject plan) throws MalformedURLException {
        Testplan result = new Testplan();
	    if(plan.$bslash("id") instanceof JsString) {
		    result.id = new ObjectId(JSONHelper.JsStringToString(plan.$bslash("id")));
	    } else {
		    result.id = new ObjectId();
	    }
	    if(plan.$bslash("user") instanceof JsObject) {
		    result.user = User.fromJSON((JsObject)plan.$bslash("user"));
	    }
	    result.path = new URL(JSONHelper.JsStringToString(plan.$bslash("path"))); // TODO make more flexible (protocol required)
		result.connectionType = ConnectionType.valueOf(JSONHelper.JsStringToString(plan.$bslash("connectionType")));
	    result.numRuns = ((JsNumber)plan.$bslash("numRuns")).value().intValue();
	    result.parallelity = ((JsNumber)plan.$bslash("parallelity")).value().intValue();
	    result.waitBetweenMsgs =
		    plan.$bslash("waitBetweenMsgs") instanceof JsNumber
			?   ((JsNumber)plan.$bslash("waitBetweenMsgs")).value().intValue()
	        :   0;
	    result.waitBeforeStart =
		    plan.$bslash("waitBeforeStart") instanceof JsNumber
	        ?   ((JsNumber)plan.$bslash("waitBeforeStart")).value().intValue()
		    :   0;
	    return result;
    }

	public JsObject toJSON() {
		return toJSON(true);
	}

    public JsObject toJSON(boolean withUser) {
        ArrayList<Tuple2<String, JsValue>> tuplesj = new ArrayList<>();
        tuplesj.add(Tuple2.apply("id", JsString$.MODULE$.apply(id.toString())));
	    tuplesj.add(Tuple2.apply("path", JsString$.MODULE$.apply(path.toString())));
	    if(withUser) {
		    tuplesj.add(Tuple2.apply("user", user.toJSON()));
	    }
	    tuplesj.add(Tuple2.apply("connectionType", JsString$.MODULE$.apply(connectionType.toString())));
	    tuplesj.add(Tuple2.apply("numRuns", JsNumber$.MODULE$.apply(new BigDecimal(new java.math.BigDecimal(numRuns)))));
	    tuplesj.add(Tuple2.apply("parallelity", JsNumber$.MODULE$.apply(new BigDecimal(new java.math.BigDecimal(parallelity)))));
	    tuplesj.add(Tuple2.apply("waitBetweenMsgs", JsNumber$.MODULE$.apply(new BigDecimal(new java.math.BigDecimal(waitBetweenMsgs)))));
	    tuplesj.add(Tuple2.apply("waitBeforeStart", JsNumber$.MODULE$.apply(new BigDecimal(new java.math.BigDecimal(waitBeforeStart)))));
        return new JsObject(JavaConversions.asScalaBuffer(tuplesj));
    }
}
