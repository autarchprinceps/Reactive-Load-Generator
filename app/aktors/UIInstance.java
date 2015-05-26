package aktors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import aktors.messages.*;
import helper.JSONHelper;
import org.bson.types.ObjectId;
import play.api.libs.json.JsObject;
import play.api.libs.json.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Patrick Robinson on 07.05.2015.
 */
public class UIInstance extends UntypedActor {
	private class RunnerConnector extends UntypedActor {
		public RunnerConnector(Testplan plan) {
			this.testplan = plan;
		}

		private Testplan testplan;

		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof LoadWorkerRaw) {
				websocket.tell(JSONHelper.objectResponse(
					"raw"
				,   ((LoadWorkerRaw)message).toJSON(false)
				), getSelf());
			} else if(message instanceof Testrun) {
				websocket.tell(JSONHelper.objectResponse(
					"runstart"
				,   ((Testrun)message).toJSON(true)
				), getSelf());
			} else {
				unhandled(message);
			}
		}
	}
	private class RunLoader extends UntypedActor {
		private Testrun testrun;

		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof Testrun) {
				testrun = (Testrun)message;
				websocket.tell(JSONHelper.objectResponse("testrun", testrun.toJSON()), getSelf());
				DBGetCMD dbGetCMD = new DBGetCMD();
				dbGetCMD.t = DBGetCMD.Type.RunRaws;
				dbGetCMD.id = testrun.getID();
				db.tell(dbGetCMD, getSelf());
			} else if(message instanceof LoadWorkerRaw) {
				websocket.tell(JSONHelper.objectResponse(
					"raw"
				,   ((LoadWorkerRaw)message).toJSON(false)
				), getSelf());
			} else {
				unhandled(message);
			}
		}
	}

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
		db = as.actorOf(Props.create(DB.class), testing ? "junit_loadgen" : "loadgen");
	}

	private void ws(JsObject message, ActorRef sender) {
		websocket.tell(message.toString(), sender);
	}



	private User currentUser;
	private List<ActorRef> running = new ArrayList<>();

	@Override
	public void onReceive(Object message) throws Exception {
		System.out.println(message);
		if(message instanceof String) {
			JsObject json = ((JsObject)Json.parse((String) message));
			String type = JSONHelper.JsStringToString(json.$bslash("type"));
			switch(type) {
				case "register":
					String name = JSONHelper.JsStringToString(json.$bslash("name"));
					User user = new User(
						new ObjectId()
					,   name
					,   JSONHelper.JsStringToString(json.$bslash("password"))
					);
					db.tell(user, getSelf());
					ws(JSONHelper.simpleResponse("registered", "Registered " + name), getSelf());
					break;
				case "login":
					DBQuery dbQuery = new DBQuery();
					dbQuery.t = DBQuery.Type.Login;
					dbQuery.terms = new HashMap<>();
					dbQuery.terms.put("name", JSONHelper.JsStringToString(json.$bslash("name")));
					dbQuery.terms.put("password", JSONHelper.JsStringToString(json.$bslash("password")));
					db.tell(dbQuery, getSelf());
					break;
				case "logout":
					currentUser = null; // TODO need to stop something else?
					// TODO disconnect running TestRuns from socket
					ws(JSONHelper.simpleResponse("logout", "Logged out"), getSelf());
					break;
				case "store plan":
					if (currentUser != null) {
						Testplan tp = Testplan.fromJSON((JsObject) json.$bslash("testplan"));
						tp.setUser(currentUser);
						db.tell(tp, getSelf()); // TODO OK response necessary?
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
					}
					break;
				case "start run":
					if (currentUser != null) {
						Testplan testplan = Testplan.fromJSON((JsObject) json.$bslash("testplan")); // TODO replace by DB lookup
						testplan.setUser(currentUser);
						ActorRef newRunner = as.actorOf(Props.create(
							LoadRunner.class
						,   as
						,   testplan
						,   db
						,   as.actorOf(Props.create(RunnerConnector.class, testplan))
						));
						newRunner.tell(RunnerCMD.Start, getSelf()); // TODO seperate start necessary? depends on UI
						running.add(newRunner);
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
					}
					break;
				case "all plans":
					if (currentUser != null) {
						DBGetCMD dbGetCMD2 = new DBGetCMD();
						dbGetCMD2.t = DBGetCMD.Type.AllPlansForUser;
						dbGetCMD2.id = currentUser.id;
						db.tell(dbGetCMD2, getSelf());
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
					}
					break;
				case "load plan": // TODO does not seem to work
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
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
					}
					break;
				case "load run":
					if(currentUser != null) {
						DBGetCMD dbGetCMD1 = new DBGetCMD();
						dbGetCMD1.t = DBGetCMD.Type.RunByID;
						dbGetCMD1.id = new ObjectId(JSONHelper.JsStringToString(json.$bslash("id")));
						db.tell(dbGetCMD1, as.actorOf(Props.create(RunLoader.class)));
					} else {
						ws(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
					}
					break;
				default:
					websocket.tell("Wrong type: " + type, getSelf());
					websocket.tell(json.fields().toString(), getSelf());
					// throw new Exception("Wrong input to WebSocket");
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
		} else if(message instanceof DBQuery) {
			DBQuery queryResult = (DBQuery)message;
			switch(queryResult.t) {
				case Login:
					if(queryResult.flag) {
						currentUser = (User)queryResult.result;
						ws(JSONHelper.simpleResponse("login", "Login successful"), getSelf());
					} else {
						currentUser = null;
						ws(JSONHelper.simpleResponse("error", "Login failed"), getSelf());
					}
					break;
			}
		} else {
			unhandled(message);
		}
	}

	@Override
	public void postStop() throws Exception {
		// TODO How is UIInstance closed? WebSocket.onclose?
		db.tell("close", getSelf());
	}
}
