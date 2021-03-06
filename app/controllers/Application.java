package controllers;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import aktors.LoadWorker;
import aktors.UIInstance;
import aktors.messages.Testplan;
import aktors.messages.Testrun;
import aktors.messages.ConnectionType;
import org.bson.types.ObjectId;
import play.mvc.*;
import tests.Test;
import views.html.index;
import views.html.tws;

public class Application extends Controller {

    public static Result index() {
	    return ok(index.render("hello"));
    }

    public static Result tws() {
        return ok(tws.render("Testing Websocket"));
    }

    public static Result startTest() throws MalformedURLException {
    	//initialize first Test directly on a loadworker
        final ActorSystem system = ActorSystem.create("loadgenerator");
        final ActorRef loadworker = system.actorOf(Props.create(LoadWorker.class), "loadworker");

        // Create the "actor-in-a-box" (not needed now)
        final Inbox inbox = Inbox.create(system);

        //create test testplan and run
        Testplan myTestplan = new Testplan(new ObjectId(), 1, 1, new URL("http://www.google.de"), 0, 0, ConnectionType.HTTP, null);
        Testrun myTestrun = new Testrun(new ObjectId(), new ArrayList<ActorRef>(), myTestplan);

        //let loadworker work
        loadworker.tell(myTestrun, ActorRef.noSender());

    	return redirect(routes.Application.index());
    }

    public static WebSocket<String> socket() {
        return WebSocket.withActor(UIInstance::props);
    }

	public static WebSocket<String> echo() {
		return WebSocket.whenReady((in, out) -> {
			in.onMessage(out::write);
			out.write("Hallo");
		});
	}

    public static WebSocket<String> test() {
	    return WebSocket.whenReady((in, out) ->
		    in.onMessage((message) -> {
			    List<String> problems;
			    switch(message) {
				    case "db":
					    out.write("DBTest started");
					    problems = Test.dbTest();
					    out.write("DBTest finished");
					    break;
				    case "uii":
					    out.write("UIITest started");
					    problems = Test.testUII();
					    out.write("UIITest finished");
					    break;
				    default:
					    out.write("Invalid test");
					    return;
			    }
			    problems.parallelStream().forEach((problem) -> out.write(problem));
		    })
	    );
    }
}
