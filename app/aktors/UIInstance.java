package aktors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import aktors.messages.*;
import helper.JSONHelper;
import org.bson.types.ObjectId;
import play.api.libs.json.JsObject;

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
				dbGetCMD.id = testrun.id;
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

	public static Props props(ActorRef out) {
		return Props.create(UIInstance.class, out);
	}

	private final ActorRef websocket;
	private final ActorSystem as;
	private final ActorRef db;

	public UIInstance(ActorRef out) {
		this.websocket = out;
		as = ActorSystem.create();
		db = as.actorOf(Props.create(DB.class));
	}

	private User currentUser;
	private List<ActorRef> running = new ArrayList<>();

	@Override
	public void onReceive(Object message) throws Exception {
		if(message instanceof JsObject) {
			JsObject json = (JsObject)message;
			String type = json.$bslash("type").toString();
			switch(type) {
				case "register":
					String name = json.$bslash("name").toString();
					User user = new User(
						new ObjectId()
					,   name
					,   json.$bslash("password").toString()
					);
					db.tell(user, getSelf());
					websocket.tell(JSONHelper.simpleResponse("registered", "Registered " + name), getSelf());
					break;
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
					websocket.tell(JSONHelper.simpleResponse("logout", "Logged out"), getSelf());
					break;
				case "store plan":
					if (currentUser != null) {
						db.tell(Testplan.fromJSON((JsObject) json.$bslash("testplan")), getSelf()); // TODO OK response necessary?
					} else {
						websocket.tell(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						throw new Exception("notauth");
					}
					break;
				case "start run":
					if (currentUser != null) {
						Testplan testplan = Testplan.fromJSON((JsObject) json.$bslash("testplan"));
						ActorRef newRunner = as.actorOf(Props.create(
							LoadRunner.class
							, as
							, testplan
							, db
							, as.actorOf(Props.create(RunnerConnector.class, testplan))
						));
						newRunner.tell(RunnerCMD.Start, getSelf()); // TODO seperate start necessary? depends on UI
						running.add(newRunner);
					} else {
						websocket.tell(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						throw new Exception("notauth");
					}
					break;
				case "all plans":
					if (currentUser != null) {
						DBGetCMD dbGetCMD2 = new DBGetCMD();
						dbGetCMD2.t = DBGetCMD.Type.AllPlansForUser;
						dbGetCMD2.id = currentUser.id;
					} else {
						websocket.tell(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						throw new Exception("notauth");
					}
					break;
				case "load plan":
					if(currentUser != null) {
						DBGetCMD dbGetCMD = new DBGetCMD();
						dbGetCMD.t = DBGetCMD.Type.PlanByID;
						dbGetCMD.id = new ObjectId(json.$bslash("id").toString());
						db.tell(dbGetCMD, getSelf());
						dbGetCMD.t = DBGetCMD.Type.AllRunsForPlan;
						db.tell(dbGetCMD, getSelf());
					} else {
						websocket.tell(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						throw new Exception("notauth");
					}
					break;
				case "load run":
					if(currentUser != null) {
						DBGetCMD dbGetCMD1 = new DBGetCMD();
						dbGetCMD1.t = DBGetCMD.Type.RunByID;
						dbGetCMD1.id = new ObjectId(json.$bslash("id").toString());
						db.tell(dbGetCMD1, as.actorOf(Props.create(RunLoader.class)));
					} else {
						websocket.tell(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
						throw new Exception("notauth");
					}
					break;
				default:
					throw new Exception("Wrong input to WebSocket");
			}
		} else if(message instanceof Testrun) {
			if(currentUser != null) {
				websocket.tell(JSONHelper.objectResponse("testrun", ((Testrun) message).toJSON()), getSelf());
			} else {
				websocket.tell(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
				throw new Exception("notauth");
			}
		} else if(message instanceof Testplan) {
			if(currentUser != null) {
				websocket.tell(JSONHelper.objectResponse("testplan", ((Testplan) message).toJSON()), getSelf());
			} else {
				websocket.tell(JSONHelper.simpleResponse("not auth", "Not authenticated"), getSelf());
				throw new Exception("notauth");
			}
		} else if(message instanceof DBQuery) {
			DBQuery queryResult = (DBQuery)message;
			switch(queryResult.t) {
				case Login:
					if(queryResult.flag) {
						currentUser = (User)queryResult.result;
						websocket.tell(JSONHelper.simpleResponse("login", "Login successful"), getSelf());
					} else {
						currentUser = null;
						websocket.tell(JSONHelper.simpleResponse("error", "Login failed"), getSelf());
					}
					break;
			}
		} else {
			unhandled(message);
		}
	}

	@Override
	public void postStop() throws Exception {
		db.tell("close", getSelf());
	}
}
