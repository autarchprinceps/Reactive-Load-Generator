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
import aktors.messages.Testplan.ConnectionType;
import org.bson.types.ObjectId;
import play.mvc.*;
import tests.Test;
import views.html.index;
import views.html.tws;

public class Application extends Controller {

    public static Result index() {
	    System.out.println("DEBUG: index render start");
	    return ok(index.render("hello"));
    }

    public static Result tws() {
	    System.out.println("DEBUG: tws render start");
        return ok(tws.render("Testing Websocket")); // TODO FIX Why is tws template not found?
    }

    public static Result startTest() throws MalformedURLException {
	    System.out.println("DEBUG: startTest start");


    	//initialize first Test directly on a loadworker
        final ActorSystem system = ActorSystem.create("loadgenerator");
        final ActorRef loadworker = system.actorOf(Props.create(LoadWorker.class), "loadworker");

        // Create the "actor-in-a-box" (not needed now)
        final Inbox inbox = Inbox.create(system);

        //create test testplan and run
        Testplan myTestplan = new Testplan();
        myTestplan.id = new ObjectId();
        myTestplan.numRuns = 1; // Per Parallel Worker
        myTestplan.parallelity = 1;
        myTestplan.path = new URL("http://www.google.com");
        myTestplan.waitBetweenMsgs = 0;
        myTestplan.waitBeforeStart = 0;
        myTestplan.connectionType = ConnectionType.HTTP;

        Testrun myTestrun = new Testrun();
        myTestrun.id = new ObjectId();
        myTestrun.testplan = myTestplan;
        myTestrun.subscribers = new ArrayList<ActorRef>();

        //let loadworker work
        loadworker.tell(myTestrun, ActorRef.noSender());

    	return redirect(routes.Application.index());
    }

    public static WebSocket<String> socket() {
        System.out.println("DEBUG: socket open start");
        return WebSocket.withActor(UIInstance::props);
    }

	public static WebSocket<String> echo() {
		System.out.println("DEBUG: echo open start");
		return WebSocket.whenReady((in, out) -> {
			System.out.println("DEBUG: echo opened");
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
					    problems = Test.dbTest();
					    break;
				    case "uii":
					    problems = Test.testUII();
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
