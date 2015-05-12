import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import aktors.DB;
import aktors.messages.*;
import org.bson.types.ObjectId;
import org.junit.*;

import play.twirl.api.Content;
import scala.concurrent.duration.Duration;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;


/**
*
* RunnerCMD (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class ApplicationTest {

    @Test
    public void simpleCheck() {
        System.out.println("simpleCheck");
        // assertThat(false);
        int a = 1 + 1;
        assertThat(a).isEqualTo(5);
    }

    @Test
    public void renderTemplate() {
        System.out.println("renderTemplate");
        // assertThat(false);
        Content html = views.html.index.render("Your new application is ready.");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Your new application is ready.");
    }

    @Test
    public void dbTest() {
        System.out.println("dbTest");
        // assertThat(false);
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
        for(int i = 0; i < 200; i++) {

            String name = "" + alphabet.charAt(i % alphabet.length());
            String password = "" + alphabet.charAt(i % alphabet.length());
            for(int j = 0; j < i / 10 + 5; j++) {
                name += alphabet.charAt((i + j + random.nextInt(i)) % alphabet.length());
                password += alphabet.charAt((i + j + random.nextInt(i)) % alphabet.length());
            }
            User tmp = new User(
                new ObjectId()
            ,   name
            ,   password
            );
	        inbox.send(db_ref, tmp);
	        users.add(tmp);
        }


        // insert plans
        for(int i = 0; i < 10000; i++) {
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
		        assertThat(false);
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
                int rstart = random.nextInt(i);
                LoadWorkerRaw tmp = new LoadWorkerRaw(
                        testrun
                    ,   i / testrun.testplan.parallelity
                    ,   rstart
                    ,   rstart + random.nextInt(i / 2)
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

        for(int i = 0; i < users.size(); i++) {
            User u = (User)inbox.receive(Duration.create(1, TimeUnit.MINUTES));
            assertThat(users.parallelStream().filter(user -> user.id == u.id && user.name == u.name && u.check(user.getPassword())).count()).isEqualTo(1);
        }

        // get plans
        plans.parallelStream().map(testplan -> {
            DBGetCMD result = new DBGetCMD();
            result.t = DBGetCMD.Type.PlanByID;
            result.id = testplan.id;
            return result;
        }).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));

        for(int i = 0; i < plans.size(); i++) {
            Testplan p = (Testplan)inbox.receive(Duration.create(1, TimeUnit.MINUTES));
            assertThat(plans.parallelStream().filter(plan -> plan.equals(p)).count()).isEqualTo(1); // TODO Does equals have to be implemented manually in Testplan ?!
        }

        // get runs
        runs.parallelStream().map(testrun -> {
            DBGetCMD result = new DBGetCMD();
            result.t = DBGetCMD.Type.RunByID;
            result.id = testrun.id;
            return result;
        }).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));

        for(int i = 0; i < plans.size(); i++) {
            Testrun r = (Testrun)inbox.receive(Duration.create(1, TimeUnit.MINUTES));
            assertThat(runs.parallelStream().filter(run -> run.equals(r)).count()).isEqualTo(1); // TODO Does equals have to be implemented manually ?!
        }

        // get raw
        runs.parallelStream().forEach(testrun1 -> {
            DBGetCMD result = new DBGetCMD();
            result.t = DBGetCMD.Type.RunRaws;
            result.id = testrun1.id;
            inbox.send(db_ref, result);
        });
        int totalrunraws = runs.parallelStream().map(testrun1 -> testrun1.testplan.parallelity * testrun1.testplan.numRuns).reduce(0, Integer::sum);
        for(int i = 0; i < totalrunraws; i++) {
            LoadWorkerRaw tmp = (LoadWorkerRaw)inbox.receive(Duration.create(1, TimeUnit.MINUTES));
            assertThat(raws.get(tmp.testrun()).contains(tmp));
        }

	    // get all plans for user
        List<Testplan> testplanList = new ArrayList<>(plans.size());
        users.parallelStream().map(user -> {
            DBGetCMD result = new DBGetCMD();
            result.t = DBGetCMD.Type.AllPlansForUser;
            result.id = user.id;
            return result;
        }).forEach(dbGetCMD -> inbox.send(db_ref, dbGetCMD));

        for(int i = 0; i < plans.size(); i++) {
            Testplan tmp = (Testplan)inbox.receive(Duration.create(1, TimeUnit.MINUTES));
            testplanList.add(tmp);
        }
        testplanList.sort((t1, t2) -> t1.id.compareTo(t2.id));
        List<Testplan> copy = new ArrayList<>(plans.size());
        Collections.copy(copy, plans);
        copy.sort((t1, t2) -> t1.id.compareTo(t2.id));
        assertThat(testplanList).isEqualTo(copy);

        // get all run for plan
        List<Testrun> testrunList = new ArrayList<>(runs.size());
        plans.parallelStream().forEach(testplan1 -> {
            DBGetCMD dbGetCMD = new DBGetCMD();
            dbGetCMD.id = testplan1.id;
            dbGetCMD.t = DBGetCMD.Type.AllRunsForPlan;
            inbox.send(db_ref, dbGetCMD);
        });
        for(int i = 0; i < runs.size(); i++) {
            testrunList.add((Testrun)inbox.receive(Duration.create(1, TimeUnit.MINUTES)));
        }
        testrunList.sort((t1, t2) -> t1.id.compareTo(t2.id));
        List<Testrun> rcpy = new ArrayList<>(runs.size());
        Collections.copy(runs, rcpy);
        rcpy.sort((t1, t2) -> t1.id.compareTo(t2.id));
        assertThat(testrunList).isEqualTo(rcpy);

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
    }
}
