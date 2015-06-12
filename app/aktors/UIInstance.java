package aktors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import aktors.messages.*;
import helper.JSONHelper;
import org.bson.types.ObjectId;
import play.api.libs.json.JsArray;
import play.api.libs.json.JsArray$;
import play.api.libs.json.JsObject;
import play.api.libs.json.Json;
import scala.collection.mutable.ArrayBuffer;

import javax.smartcardio.TerminalFactorySpi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Patrick Robinson on 07.05.2015.
 */
public class UIInstance extends UntypedActor {
	public static Props props(ActorRef out) { return props(out, false); }

	public static Props props(ActorRef out, boolean testing) {
		return Props.create(UIInstance.class, out, testing);
	}

	private final ActorRef websocket;
	private final ActorSystem as;
	private final ActorRef db;

	public UIInstance(ActorRef out, boolean testing) {
		this.websocket = out;
		as = ActorSystem.create();
		db = as.actorOf(Props.create(DB.class, testing ? "junit_loadgen" : "loadgen"));
	}

	private void ws(JsObject message, ActorRef sender) {
		websocket.tell(message.toString(), sender);
	}

	private User currentUser;
	private List<ActorRef> running = new ArrayList<>();

	@Override
	public void onReceive(Object message) throws Exception {
		System.out.println("DEBUG: UIInstance " + message);
		if(message instanceof String) {
			JsObject json = ((JsObject)Json.parse((String) message));
			String type = JSONHelper.JsStringToString(json.$bslash("type"));
			switch(type) {
				case "register": // Tested
					DBQuery regQuery = new DBQuery();
					regQuery.t = DBQuery.Type.Register;
					regQuery.terms = new HashMap<>();
					regQuery.terms.put("name", JSONHelper.JsStringToString(json.$bslash("name")));
					regQuery.terms.put("password", JSONHelper.JsStringToString(json.$bslash("password")));
					db.tell(regQuery, getSelf());
					break;
				case "login": // Tested
					DBQuery dbQuery = new DBQuery();
					dbQuery.t = DBQuery.Type.Login;
					dbQuery.terms = new HashMap<>();
					dbQuery.terms.put("name", JSONHelper.JsStringToString(json.$bslash("name")));
					dbQuery.terms.put("password", JSONHelper.JsStringToString(json.$bslash("password")));
					db.tell(dbQuery, getSelf());
					break;
				case "logout":
					System.out.println("DEBUG: UIInstance Logging out");
					currentUser = null; // TODO need to stop something else?
					// TODO disconnect running TestRuns from socket
					ws(JSONHelper.simpleResponse("logout", "Logged out"), getSelf());
					System.out.println("DEBUG: UIInstance Logged out");
					break;
				case "store plan": // Tested
					System.out.println("DEBUG: UIInstance Storing plan");
					if (currentUser != null) {
						Testplan tp = Testplan.fromJSON((JsObject) json.$bslash("testplan"));
						tp.setUser(currentUser);
						db.tell(tp, getSelf()); // TODO OK response necessary?
						System.out.println("DEBUG: UIInstance Stored plan");
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						System.out.println("DEBUG: UIInstance NotAuth");
					}
					break;
				case "start run": // Tested
					System.out.println("DEBUG: UIInstance Starting run");
					if (currentUser != null) {
						DBGetCMD dbGetCMD3 = new DBGetCMD();
						dbGetCMD3.t = DBGetCMD.Type.PlanByID;
						dbGetCMD3.id = new ObjectId(JSONHelper.JsStringToString(json.$bslash("testplan")));
						dbGetCMD3.callback = (testplan) -> {
							System.out.println("DEBUG UIIn WS: " + websocket.toString());
							ActorRef newRunner = as.actorOf(Props.create(
								LoadRunner.class
								, as
								, testplan
								, db
								, as.actorOf(Props.create(RunnerConnector.class, websocket))
							));
							newRunner.tell(RunnerCMD.Start, getSelf()); // TODO seperate start necessary? depends on UI
							running.add(newRunner);
						};
						db.tell(dbGetCMD3, getSelf());
						System.out.println("DEBUG: UIInstance Started run");
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						System.out.println("DEBUG: UIInstance NotAuth");
					}
					break;
				case "all plans": // Tested
					System.out.println("DEBUG: UIInstance Getting all plans");
					if (currentUser != null) {
						DBGetCMD dbGetCMD2 = new DBGetCMD();
						dbGetCMD2.t = DBGetCMD.Type.AllPlansForUser;
						dbGetCMD2.id = currentUser.getID();
						db.tell(dbGetCMD2, getSelf());
						System.out.println("DEBUG: UIInstance Got all plans");
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						System.out.println("DEBUG: UIInstance NotAuth");
					}
					break;
				case "load plan": // Tested (both parts)
					System.out.println("DEBUG: UIInstance Loading plan");
					if(currentUser != null) {
						DBGetCMD dbGetCMD = new DBGetCMD();
						dbGetCMD.t = DBGetCMD.Type.PlanByID;
						dbGetCMD.id = new ObjectId(JSONHelper.JsStringToString(json.$bslash("id")));
						System.out.println(dbGetCMD.id.toString());
						db.tell(dbGetCMD, getSelf());
						DBGetCMD dbGetCMD2 = new DBGetCMD();
						dbGetCMD2.t = DBGetCMD.Type.AllRunsForPlan;
						dbGetCMD2.id = dbGetCMD.id;
						db.tell(dbGetCMD2, getSelf());
						System.out.println("DEBUG: UIInstance Loaded plan");
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						System.out.println("DEBUG: UIInstance NotAuth");
					}
					break;
				case "load run": // Tested
					System.out.println("DEBUG: UIInstance Loading run");
					if(currentUser != null) {
						DBGetCMD dbGetCMD1 = new DBGetCMD();
						dbGetCMD1.t = DBGetCMD.Type.RunByID;
						dbGetCMD1.id = new ObjectId(JSONHelper.JsStringToString(json.$bslash("id")));
						db.tell(dbGetCMD1, as.actorOf(Props.create(RunLoader.class, websocket, db))); // TODO seperate db ?
						System.out.println("DEBUG: UIInstance Loaded run");
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						System.out.println("DEBUG: UIInstance NotAuth");
					}
					break;
				default:
					websocket.tell("Wrong type: " + type, getSelf());
					websocket.tell(json.fields().toString(), getSelf());
					System.err.println("ERROR: UIInstance Wrong type: " + type);
					// throw new Exception("Wrong input to WebSocket");
					break;
			}
		} else if(message instanceof Testrun) {
			if(currentUser != null) {
				ws(JSONHelper.objectResponse("testrun", ((Testrun) message).toJSON()), getSelf());
			} else {
				ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
			}
		} else if(message instanceof Testplan) {
			if(currentUser != null) {
				ws(JSONHelper.objectResponse("testplan", ((Testplan) message).toJSON()), getSelf());
			} else {
				ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
			}
		} else if(message instanceof JsObject) {
			ws((JsObject)message, getSelf());
		} else if(message instanceof DBQuery) {
			DBQuery queryResult = (DBQuery)message;
			switch(queryResult.t) {
				case Login: // Tested
					if(queryResult.flag) {
						currentUser = (User)queryResult.result;
						ws(JSONHelper.simpleResponse("login", "Login successful"), getSelf());
					} else {
						currentUser = null;
						ws(JSONHelper.simpleResponse("error", "Login failed: " + queryResult.result), getSelf());
					}
					break;
				case Register: // Tested
					if(queryResult.flag) {
						ws(JSONHelper.simpleResponse("register", "Register successful"), getSelf());
					} else {
						ws(JSONHelper.simpleResponse("error", "Register failed: " + queryResult.result), getSelf());
					}
					break;
			}
		} else {
			System.out.println("DEBUG: UIInstance unhandled");
			unhandled(message);
		}
	}

	@Override
	public void postStop() throws Exception {
		db.tell("close", getSelf());
	}
}
