package aktors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import aktors.messages.DBGetCMD;
import aktors.messages.DBQuery;
import aktors.messages.User;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsValue;

import scala.Tuple2;
import scala.collection.Map;
import scala.collection.Seq;
import scala.collection.Seq$;

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
					currentUser = null;
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

					} else {
						Seq<Tuple2<String, JsValue>> jsans = new Seq<>(){};
						JsObject wsans = new JsObject(jsans);
						out.tell();
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
