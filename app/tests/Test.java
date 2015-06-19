package tests;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import aktors.DB;
import aktors.messages.*;
import com.typesafe.config.ConfigFactory;
import helper.JSONHelper;
import org.bson.types.ObjectId;
import play.api.libs.json.JsArray;
import play.api.libs.json.JsObject;
import play.api.libs.json.JsValue;
import scala.collection.Seq;
import scala.concurrent.duration.Duration;
import scala.util.parsing.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Patrick Robinson on 21.05.15.
 */
public class Test {
	public static String problem(StackTraceElement ex, String str) {
		String problem = ex.getClassName() + "/" + ex.getMethodName() + ":" + ex.getLineNumber() + "-> " + str;
		System.err.println("ERROR: " + problem);
		return problem;
	}

	public static List<String> dbTest() {
		System.out.println("Test.java: dbTest");
		List<String> problems = new LinkedList<>();
		try {
			Random random = new Random();
			ActorSystem as = ActorSystem.create(
				"Test"
			,   ConfigFactory.load(
					ConfigFactory.parseString("akka.actor.dsl.inbox-size=1000000")
						.withFallback(ConfigFactory.load())
				));
			ActorRef db_ref = as.actorOf(Props.create(DB.class, "junit_loadgen"));
			Inbox inbox = Inbox.create(as);

			List<User> users = new ArrayList<>();
			List<Testplan> plans = new ArrayList<>();
			List<Testrun> runs = new ArrayList<>();

			// insert users
			String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
			for(int i = 0; i < 20; i++) {
				String name = "" + alphabet.charAt(i % alphabet.length());
				String password = "" + alphabet.charAt(i % alphabet.length());
				for(int j = 0; j < i / 10 + 5; j++) {
					name += alphabet.charAt((i + j + random.nextInt(i + 1)) % alphabet.length());
					password += alphabet.charAt((i + j + random.nextInt(i + 1)) % alphabet.length());
				}
				DBQuery regQuery = new DBQuery();
				regQuery.t = DBQuery.Type.Register;
				regQuery.terms = new HashMap<>();
				regQuery.terms.put("name", name);
				regQuery.terms.put("password", password);
				inbox.send(db_ref, regQuery);
				DBQuery response = (DBQuery)inbox.receive(Duration.create(200, TimeUnit.MINUTES));
				if(response.flag) {
					User tmp = new User(
						(ObjectId)response.result
					,   name
					,   password
					);
					users.add(tmp);
				} else {
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"Register failed: " + response
					));
				}
			}
			System.out.println("dbTest users created, starting insert plans");
			// Thread.sleep(1000);
			// insert plans
			for(int i = 0; i < 100; i++) {
				Testplan tmp = new Testplan(
					new ObjectId(),
					1, //+ random.nextInt(5),
					1, //+ random.nextInt(10),
					new URL("http://example.com:1337/test/blub"), // TODO autogen?
					random.nextInt(10),
					random.nextInt(10),
					ConnectionType.values()[random.nextInt(ConnectionType.values().length)],
					null
				);
				tmp.setUser(users.get(random.nextInt(users.size())));
				inbox.send(db_ref, tmp);
				plans.add(tmp);
			}
			// return reciept
			for(int i = 0; i < 100; i++) {
				JsObject get = (JsObject)inbox.receive(Duration.create(200, TimeUnit.MINUTES));
				if(!plans.parallelStream().map(testplan ->
					testplan.getID().equals(new ObjectId(JSONHelper.JsStringToString(get.$bslash("id"))))
				).reduce(false, Boolean::logicalOr)) {
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"Wrong return receipt: " + get
					));
				}
			}
			System.out.println("dbTest plans inserted, starting insert runs");
			// Thread.sleep(10000);
			// insert runs
			plans.stream().forEach(testplan1 -> {
				for (int i = 0; i < 3; i++) {
					Testrun tmp = new Testrun(new ObjectId(), null, testplan1);
					inbox.send(db_ref, tmp);
					runs.add(tmp);
				}
			});
			System.out.println("dbTest runs inserted, starting insert raw");
			// Thread.sleep(20000);
			// insert raw
			runs.parallelStream().forEach(testrun -> {
				// List<LoadWorkerRaw> tmps = new ArrayList<>(); // TODO never queried
				Testplan tmptp = testrun.getTestplan();
				for (int i = 0; i < tmptp.getNumRuns() * tmptp.getParallelity(); i++) {
					int rstart = random.nextInt(i + 1);
					LoadWorkerRaw tmp = new LoadWorkerRaw(
						testrun
						, i / tmptp.getParallelity()
						, rstart
						, rstart + random.nextInt(i / 2 + 1)
					);
					inbox.send(db_ref, tmp);
					try {Thread.sleep(tmptp.getWaitBetweenMsgs());} catch(Exception ex) {}
					// tmps.add(tmp);
				}
			});
			System.out.println("dbTest raws inserted, starting get user");
			// Thread.sleep(100000);
			// get users
			users.stream().map((user -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.UserByID;
				result.id = user.getID();
				return result;
			})).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));
			// Thread.sleep(30000);
			for (int i = 0; i < users.size(); i++) {
				User u = (User) inbox.receive(Duration.create(200, TimeUnit.MINUTES));
				if(users.stream().filter(user -> user.getID().equals(u.getID()) && user.getName().equals(u.getName()) && u.check(user.getPassword())).count() != 1) {
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"A user doesn't match: " + u.toJSON(true).toString()
					));
				}
			}
			System.out.println("dbTest users got, starting get plans");
			// Thread.sleep(10000);
			// get plans
			plans.stream().map(testplan -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.PlanByID;
				result.id = testplan.getID();
				return result;
			}).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));
			// Thread.sleep(1000);
			for (int i = 0; i < plans.size(); i++) {
				Testplan p = (Testplan) inbox.receive(Duration.create(200, TimeUnit.MINUTES));
				if(plans.stream().filter(plan -> plan.equals(p)).count() != 1) {
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"A plan doesn't match: " + p.toJSON(true).toString()
					));
				}
			}
			System.out.println("dbTest plans got, starting get runs");
			// Thread.sleep(10000);
			// get runs
			runs.stream().forEach(testrun -> {
				DBGetCMD dbGetCMD = new DBGetCMD();
				dbGetCMD.t = DBGetCMD.Type.RunByID;
				dbGetCMD.id = testrun.getID();
				inbox.send(db_ref, dbGetCMD);
				Testrun r = (Testrun)inbox.receive(Duration.create(200, TimeUnit.MINUTES));
				if(!testrun.equals(r))
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"A run doesn't match: " + r.toJSON(true).toString()
					));
			});
			System.out.println("dbTest got runs, starting get raw");
			// Thread.sleep(10000);
			// get raw
			for(int i1 = 0; i1 < runs.size(); i1++) {
				Testrun testrun1 = runs.get(i1);
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.RunRaws;
				result.id = testrun1.getID();
				inbox.send(db_ref, result);
				int raws = testrun1.getTestplan().getParallelity() * testrun1.getTestplan().getNumRuns();
				System.out.println("dbTest run: " + i1 + " raws: " + raws + " " + testrun1.getID());
				for(int i = 0; i < raws; i++) {
					Object tmp = inbox.receive(Duration.create(5, TimeUnit.MINUTES));
					if(!(tmp instanceof LoadWorkerRaw)) {
						problems.add(problem(
							new Exception().getStackTrace()[0],
							"A LoadWorkerRaw doesn't match: " + tmp
						));
					}
				}
				System.out.println("dbTest raws one run done");
			}
			System.out.println("dbTest got raws, starting get all plans for user");
			// Thread.sleep(10000);
			// get all plans for user
			users.stream().map(user -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.AllPlansForUser;
				result.id = user.getID();
				return result;
			}).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));
			// Thread.sleep(30000);
			List<Testplan> testplanList = new ArrayList<>(plans.size());
			JsObject get = (JsObject)inbox.receive(Duration.create(200, TimeUnit.MINUTES)); // TODO LoadWorkerRaw received
			Seq<JsValue> arr = ((JsArray)get.$bslash("content")).value();
			JsValue[] testplanArray = new JsValue[arr.size()];
			arr.copyToArray(testplanArray);
			for(JsValue value : testplanArray) {
				testplanList.add(Testplan.fromJSON((JsObject)value));
			}
			testplanList.sort((t1, t2) -> t1.getID().compareTo(t2.getID()));
			List<Testplan> copy = new ArrayList<>(plans.size());
			Collections.copy(copy, plans);
			copy.sort((t1, t2) -> t1.getID().compareTo(t2.getID()));
			if(!Arrays.deepEquals(testplanList.toArray(), copy.toArray())) {
				problems.add(problem(
					new Exception().getStackTrace()[0],
					"AllPlansForUser failed: orig size: " + copy.size() + " result size: " + testplanList.size()
				));
			}
			System.out.println("dbTest go all plans for user, starting get all run for plan");
			// Thread.sleep(10000);
			// get all run for plan
			List<Testrun> testrunList = new ArrayList<>(runs.size());
			// Thread.sleep(30000);
			plans.stream().forEach(testplan1 -> {
				DBGetCMD dbGetCMD = new DBGetCMD();
				dbGetCMD.id = testplan1.getID();
				dbGetCMD.t = DBGetCMD.Type.AllRunsForPlan;
				inbox.send(db_ref, dbGetCMD);
				JsObject get1 = (JsObject) inbox.receive(Duration.create(200, TimeUnit.MINUTES));
				if(testplan1.getID().equals(new ObjectId(JSONHelper.JsStringToString(get1.$bslash("testplan"))))) {
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"AllRuns: Testplan doesn't match"
					));
				}
				Seq<JsValue> arr1 = ((JsArray) get.$bslash("content")).value();
				JsValue[] testrunArray = new JsValue[arr.size()];
				arr1.copyToArray(testrunArray);
				for(JsValue elem : testrunArray) {
					try {
						testrunList.add(Testrun.fromJSON((JsObject) elem));
					} catch(MalformedURLException e) {
						problems.add(problem(
							e.getStackTrace()[0],
							"AllRuns: Run" + elem + ": Malformed URL"
						));
					}
				}
			});
			testrunList.sort((t1, t2) -> t1.getID().compareTo(t2.getID()));
			List<Testrun> rcpy = new ArrayList<>(runs.size());
			Collections.copy(runs, rcpy);
			rcpy.sort((t1, t2) -> t1.getID().compareTo(t2.getID()));
			if(!Arrays.deepEquals(testrunList.toArray(), rcpy.toArray())) {
				problems.add(problem(
					new Exception().getStackTrace()[0],
					"AllRunsForPlan failed: orig size: " + rcpy.size() + " result size: " + testrunList.size()
				));
			}
			System.out.println("dbTest got all runs for plan, starting delete");
			// Thread.sleep(10000);
			// delete
			runs.stream().forEach(testrun -> {
				DBDelCMD delCMD = new DBDelCMD();
				delCMD.t = DBDelCMD.Type.Run;
				delCMD.id = testrun.getID();
				inbox.send(db_ref, delCMD);
			});
			plans.stream().forEach(testplan -> {
				DBDelCMD delCMD = new DBDelCMD();
				delCMD.t = DBDelCMD.Type.Plan;
				delCMD.id = testplan.getID();
				inbox.send(db_ref, delCMD);
			});
			users.stream().forEach(user -> {
				DBDelCMD delCMD = new DBDelCMD();
				delCMD.t = DBDelCMD.Type.User;
				delCMD.id = user.getID();
				inbox.send(db_ref, delCMD);
			});
			System.out.println("dbTest deleted, starting close");
			// Thread.sleep(30000);
			inbox.send(db_ref, "close");
		} catch(Exception ex) {
			ex.printStackTrace();
			problems.add(problem(ex.getStackTrace()[0], "Other: " + ex.getMessage()));
		} finally {
			return problems;
		}
	}

	public static List<String> testUII() {
		System.out.println("Test.java: testUII");
		return new TestUIInstance().apply();
	} // TODO does nothing !
}
