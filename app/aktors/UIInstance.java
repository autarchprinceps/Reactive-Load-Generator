package aktors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import aktors.messages.DBQuery;
import aktors.messages.User;
import org.bson.types.ObjectId;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsObject$;
import play.api.libs.json.JsString$;
import play.api.libs.json.JsValue;

import scala.Tuple2;
import scala.collection.*;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Patrick Robinson on 07.05.2015.
 */
public class UIInstance extends UntypedActor {

	public static Props props(ActorRef out) {
		return Props.create(UIInstance.class, out);
	}

	private final ActorRef out;
	private final ActorSystem as;
	private final ActorRef db;

	public UIInstance(ActorRef out) {
		this.out = out;
		as = ActorSystem.create();
		db = as.actorOf(Props.create(DB.class));
	}

	private User currentUser;

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof JsObject) {
			JsObject json = (JsObject)message;
			String type = json.$bslash("type").toString();
			switch(type) {
				case "login":
					DBQuery dbQuery = new DBQuery();
					dbQuery.t = DBQuery.Type.Login;
					dbQuery.terms = new HashMap<>();
					dbQuery.terms.put("name", json.$bslash("name").toString());
					dbQuery.terms.put("password", json.$bslash("password").toString());
					db.tell(dbQuery, getSelf());
					break;
				case "logout":
					currentUser = null; // TODO need to stop something else?
					ArrayList<Tuple2<String, JsValue>> jsansj = new ArrayList<>();
					jsansj.add(Tuple2.apply("type", JsString$.MODULE$.apply("login")));
					jsansj.add(Tuple2.apply("describtion", JsString$.MODULE$.apply("Login successful")));
					out.tell(new JsObject(JavaConversions.asScalaBuffer(jsansj)), getSelf());
					break;
				case "plan":

					break;
				case "run":
					break;
				default:
					throw new Exception("Wrong input to WebSocket");
			}
		} else if(message instanceof DBQuery) {
			DBQuery queryResult = (DBQuery)message;
			switch(queryResult.t) {
				case Login:
					if(queryResult.flag) {
						currentUser = (User)queryResult.result;
						ArrayList<Tuple2<String, JsValue>> jsansj = new ArrayList<>();
						jsansj.add(Tuple2.apply("type", JsString$.MODULE$.apply("login")));
						jsansj.add(Tuple2.apply("describtion", JsString$.MODULE$.apply("Login successful")));
						out.tell(new JsObject(JavaConversions.asScalaBuffer(jsansj)), getSelf());
					} else {
						currentUser = null;
						ArrayList<Tuple2<String, JsValue>> jsansj = new ArrayList<>();
						jsansj.add(Tuple2.apply("type", JsString$.MODULE$.apply("error")));
						jsansj.add(Tuple2.apply("describtion", JsString$.MODULE$.apply("Login failed")));
						out.tell(new JsObject(JavaConversions.asScalaBuffer(jsansj)), getSelf());
					}
					break;
			}
		} else {
			unhandled(message);
		}
	}

	@Override
	public void postStop() throws Exception {

	}
}
