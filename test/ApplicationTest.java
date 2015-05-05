import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import aktors.DB;
import aktors.messages.*;
import com.fasterxml.jackson.databind.JsonNode;
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
        int a = 1 + 1;
        assertThat(a).isEqualTo(2);
    }

    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("Your new application is ready.");
        assertThat(contentType(html)).isEqualTo("text/html");
        assertThat(contentAsString(html)).contains("Your new application is ready.");
    }

    @Test
    public void dbTest() {
        ActorSystem as = ActorSystem.create();
        DB db = new DB("junit_loadgen"); // TODO bind to ActorSystem? or tell actorsystem to use non empty constructor
        ActorRef db_ref = db.getSelf();
        Inbox inbox = Inbox.create(as);

        List<User> users = new ArrayList<>();
        List<Testplan> plans = new ArrayList<>();
        List<Testrun> runs = new ArrayList<>();
        Map<Testrun, List<LoadWorkerRaw>> raws = new HashMap<>();

        // insert users
        // TODO
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

        // insert plans
        // TODO
        // get all plans for user
        // TODO how?
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

        // insert runs
        // TODO
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

        // insert raw
        Random random = new Random();
        runs.parallelStream().forEach(testrun -> {
            List<LoadWorkerRaw> tmps = new ArrayList<>();
            for (int i = 0; i < 20000; i++) {
                LoadWorkerRaw tmp = new LoadWorkerRaw();
                tmp.testrun = testrun;
                tmp.start = random.nextInt(i);
                tmp.end = tmp.start + random.nextInt(i / 2);
                tmp.iterOnWorker = i;
                tmps.add(tmp);
            }
            raws.put(testrun, tmps);
        });
        raws.forEach((testrun, loadWorkerRaws) -> loadWorkerRaws.parallelStream().forEach(loadWorkerRaw -> inbox.send(db_ref, loadWorkerRaw)));

        for(int i = 0; i < raws.size() * 20000; i++) {
            LoadWorkerRaw tmp = (LoadWorkerRaw)inbox.receive(Duration.create(1, TimeUnit.MINUTES));
            assertThat(raws.get(tmp.testrun).contains(tmp));
        }

        inbox.send(db_ref, "close");
    }
}
