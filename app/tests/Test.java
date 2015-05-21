package tests;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import aktors.DB;
import aktors.messages.*;
import org.bson.types.ObjectId;
import scala.concurrent.duration.Duration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Patrick on 21.05.15.
 */
public class Test {
	public static String problem(StackTraceElement ex, String str) {
		return ex.getClassName() + "/" + ex.getMethodName() + ":" + ex.getLineNumber() + "-> " + str;
	}

	public static List<String> dbTest() {
		System.out.println("dbTest");
		List<String> problems = new LinkedList<>();
		try {
			Random random = new Random();
			ActorSystem as = ActorSystem.create();
			ActorRef db_ref = as.actorOf(Props.create(DB.class, "junit_loadgen"));
			Inbox inbox = Inbox.create(as);

			List<User> users = new ArrayList<>();
			List<Testplan> plans = new ArrayList<>();
			List<Testrun> runs = new ArrayList<>();
			Map<Testrun, List<LoadWorkerRaw>> raws = new HashMap<>();

			// insert users
			String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
			for (int i = 0; i < 200; i++) {
				String name = "" + alphabet.charAt(i % alphabet.length());
				String password = "" + alphabet.charAt(i % alphabet.length());
				for (int j = 0; j < i / 10 + 5; j++) {
					name += alphabet.charAt((i + j + random.nextInt(i + 1)) % alphabet.length());
					password += alphabet.charAt((i + j + random.nextInt(i + 1)) % alphabet.length());
				}
				User tmp = new User(
					new ObjectId()
					, name
					, password
				);
				inbox.send(db_ref, tmp);
				users.add(tmp);
			}


			// insert plans
			for (int i = 0; i < 10000; i++) {
				Testplan tmp = new Testplan();
				tmp.user = users.get(random.nextInt(users.size()));
				tmp.connectionType = Testplan.ConnectionType.values()[random.nextInt(Testplan.ConnectionType.values().length)];
				tmp.numRuns = random.nextInt(20000) + random.nextInt(20000) + random.nextInt(20000);
				tmp.parallelity = 1 + random.nextInt(1000);
				tmp.id = new ObjectId();
				tmp.waitBeforeStart = random.nextInt(10);
				tmp.waitBetweenMsgs = random.nextInt(10);
				try {
					tmp.path = new URL("http://example.com:1337/test/blub"); // TODO autogen?
				} catch (MalformedURLException ex) {
					ex.printStackTrace();
					problems.add(problem(ex.getStackTrace()[0], "URL Malformed (Error in test, not code)"));
				}
				inbox.send(db_ref, tmp);
				plans.add(tmp);
			}

			// insert runs
			plans.parallelStream().forEach(testplan1 -> {
				for (int i = 0; i < 5; i++) {
					Testrun tmp = new Testrun();
					tmp.testplan = testplan1;
					tmp.id = new ObjectId();
					inbox.send(db_ref, tmp);
					runs.add(tmp);
				}
			});

			// insert raw
			runs.parallelStream().forEach(testrun -> {
				List<LoadWorkerRaw> tmps = new ArrayList<>();
				for (int i = 0; i < testrun.testplan.numRuns * testrun.testplan.parallelity; i++) {
					int rstart = random.nextInt(i + 1);
					LoadWorkerRaw tmp = new LoadWorkerRaw(
						testrun
						, i / testrun.testplan.parallelity
						, rstart
						, rstart + random.nextInt(i / 2 + 1)
					);
					inbox.send(db_ref, tmp);
					tmps.add(tmp);
				}
				raws.put(testrun, tmps);
			});
			raws.forEach((testrun, loadWorkerRaws) -> loadWorkerRaws.parallelStream().forEach(loadWorkerRaw -> inbox.send(db_ref, loadWorkerRaw)));

			// get users
			users.parallelStream().map((user -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.UserByID;
				result.id = user.id;
				return result;
			})).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));

			for (int i = 0; i < users.size(); i++) {
				User u = (User) inbox.receive(Duration.create(1, TimeUnit.MINUTES));
				if(users.parallelStream().filter(user -> user.id == u.id && user.name == u.name && u.check(user.getPassword())).count() != 1) {
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"A user doesn't match: " + u.toJSON(true).toString()
					));
				}
			}

			// get plans
			plans.parallelStream().map(testplan -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.PlanByID;
				result.id = testplan.id;
				return result;
			}).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));

			for (int i = 0; i < plans.size(); i++) {
				Testplan p = (Testplan) inbox.receive(Duration.create(1, TimeUnit.MINUTES));
				if(plans.parallelStream().filter(plan -> plan.equals(p)).count() != 1) { // TODO Does equals have to be implemented manually in Testplan ?!
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"A plan doesn't match: " + p.toJSON(true).toString()
					));
				}
			}

			// get runs
			runs.parallelStream().map(testrun -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.RunByID;
				result.id = testrun.id;
				return result;
			}).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));

			for (int i = 0; i < plans.size(); i++) {
				Testrun r = (Testrun) inbox.receive(Duration.create(1, TimeUnit.MINUTES));
				if(runs.parallelStream().filter(run -> run.equals(r)).count() != 1) { // TODO Does equals have to be implemented manually ?!
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"A run doesn't match: " + r.toJSON(true).toString()
					));
				}
			}

			// get raw
			runs.parallelStream().forEach(testrun1 -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.RunRaws;
				result.id = testrun1.id;
				inbox.send(db_ref, result);
			});
			int totalrunraws = runs.parallelStream().map(testrun1 -> testrun1.testplan.parallelity * testrun1.testplan.numRuns).reduce(0, Integer::sum);
			for (int i = 0; i < totalrunraws; i++) {
				LoadWorkerRaw tmp = (LoadWorkerRaw) inbox.receive(Duration.create(1, TimeUnit.MINUTES));
				if(!raws.get(tmp.testrun()).contains(tmp)) {
					problems.add(problem(
						new Exception().getStackTrace()[0],
						"A LoadWorkerRaw doesn't match: " + tmp.toJSON(true).toString()
					));
				}
			}

			// get all plans for user
			List<Testplan> testplanList = new ArrayList<>(plans.size());
			users.parallelStream().map(user -> {
				DBGetCMD result = new DBGetCMD();
				result.t = DBGetCMD.Type.AllPlansForUser;
				result.id = user.id;
				return result;
			}).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));

			for (int i = 0; i < plans.size(); i++) {
				Testplan tmp = (Testplan) inbox.receive(Duration.create(1, TimeUnit.MINUTES));
				testplanList.add(tmp);
			}
			testplanList.sort((t1, t2) -> t1.id.compareTo(t2.id));
			List<Testplan> copy = new ArrayList<>(plans.size());
			Collections.copy(copy, plans);
			copy.sort((t1, t2) -> t1.id.compareTo(t2.id));
			if(!Arrays.deepEquals(testplanList.toArray(), copy.toArray())) {
				problems.add(problem(
					new Exception().getStackTrace()[0],
					"AllPlansForUser failed: orig size: " + copy.size() + " result size: " + testplanList.size()
				));
			}

			// get all run for plan
			List<Testrun> testrunList = new ArrayList<>(runs.size());
			plans.parallelStream().forEach(testplan1 -> {
				DBGetCMD dbGetCMD = new DBGetCMD();
				dbGetCMD.id = testplan1.id;
				dbGetCMD.t = DBGetCMD.Type.AllRunsForPlan;
				inbox.send(db_ref, dbGetCMD);
			});
			for (int i = 0; i < runs.size(); i++) {
				testrunList.add((Testrun) inbox.receive(Duration.create(1, TimeUnit.MINUTES)));
			}
			testrunList.sort((t1, t2) -> t1.id.compareTo(t2.id));
			List<Testrun> rcpy = new ArrayList<>(runs.size());
			Collections.copy(runs, rcpy);
			rcpy.sort((t1, t2) -> t1.id.compareTo(t2.id));
			if(!Arrays.deepEquals(testrunList.toArray(), rcpy.toArray())) {
				problems.add(problem(
					new Exception().getStackTrace()[0],
					"AllRunsForPlan failed: orig size: " + rcpy.size() + " result size: " + testrunList.size()
				));
			}

			// delete
			runs.parallelStream().forEach(testrun -> {
				DBDelCMD delCMD = new DBDelCMD();
				delCMD.t = DBDelCMD.Type.Run;
				delCMD.id = testrun.id;
				inbox.send(db_ref, delCMD);
			});
			plans.parallelStream().forEach(testplan -> {
				DBDelCMD delCMD = new DBDelCMD();
				delCMD.t = DBDelCMD.Type.Plan;
				delCMD.id = testplan.id;
				inbox.send(db_ref, delCMD);
			});
			users.parallelStream().forEach(user -> {
				DBDelCMD delCMD = new DBDelCMD();
				delCMD.t = DBDelCMD.Type.User;
				delCMD.id = user.id;
				inbox.send(db_ref, delCMD);
			});

			inbox.send(db_ref, "close");
		} catch(Exception ex) {
			ex.printStackTrace();
			problems.add(problem(ex.getStackTrace()[0], "Other: " + ex.getMessage()));
		} finally {
			return problems;
		}
	}

	public static List<String> testUII() {
		return new TestUIInstance().apply();
	}
}
