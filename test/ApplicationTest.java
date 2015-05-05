import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import aktors.DB;
import aktors.messages.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.bson.types.ObjectId;
import org.junit.*;

import play.mvc.*;
import play.test.*;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.RequiredValidator;
import play.i18n.Lang;
import play.libs.F;
import play.libs.F.*;
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
        DB db = new DB("junit_loadgen"); // TODO bind to ActorSystem? or tell actorsystem to use non empty constructor
        ActorRef db_ref = db.getSelf();
        Inbox inbox = Inbox.create(as);

        List<User> users = new ArrayList<>();
        List<Testplan> plans = new ArrayList<>();
        List<Testrun> runs = new ArrayList<>();
        Map<Testrun, List<LoadWorkerRaw>> raws = new HashMap<>();

        // insert users
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        for(int i = 0; i < 200; i++) {
            User tmp = new User();
            tmp.id = new ObjectId();
            tmp.name = "" + alphabet.charAt(i % alphabet.length());
            for(int j = 0; j < i / 10 + 5; j++) {
                tmp.name += alphabet.charAt((i + j + random.nextInt(i)) % alphabet.length());
            }
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
	        tmp.testId = new ObjectId();
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
                LoadWorkerRaw tmp = new LoadWorkerRaw();
                tmp.testrun = testrun;
                tmp.start = random.nextInt(i);
                tmp.end = tmp.start + random.nextInt(i / 2);
                tmp.iterOnWorker = i / testrun.testplan.parallelity;
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
            assertThat(users.parallelStream().filter(user -> user.id == u.id && user.name == u.name).count()).isEqualTo(1);
        }

        // get plans
        plans.parallelStream().map(testplan -> {
            DBGetCMD result = new DBGetCMD();
            result.t = DBGetCMD.Type.PlanByID;
            result.id = testplan.testId;
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
        for(int i = 0; i < raws.size() * 20000; i++) {
            LoadWorkerRaw tmp = (LoadWorkerRaw)inbox.receive(Duration.create(1, TimeUnit.MINUTES));
            assertThat(raws.get(tmp.testrun).contains(tmp));
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
        testplanList.sort((t1, t2) -> t1.testId.compareTo(t2.testId));
        List<Testplan> copy = new ArrayList<>(plans.size());
        Collections.copy(copy, plans);
        copy.sort((t1, t2) -> t1.testId.compareTo(t2.testId));
        assertThat(testplanList).isEqualTo(copy);
        // get all run for plan
        // TODO

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
            delCMD.id = testplan.testId;
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
