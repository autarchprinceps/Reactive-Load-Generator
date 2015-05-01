package controllers;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import aktors.LoadRunner;
import aktors.LoadWorker;
import aktors.messages.Testplan;
import aktors.messages.Testrun;
import aktors.messages.Testplan.ConnectionType;
import play.*;
import play.data.Form;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {

    public static Result index() {
        return ok(index.render("hello"));
    }
    
    public static Result startTest() throws MalformedURLException {
    	
    	
    	
    	//initialize first Test directly on a loadworker
        final ActorSystem system = ActorSystem.create("Loadgenerator");
        final ActorRef loadworker = system.actorOf(Props.create(LoadWorker.class), "loadworker");

        // Create the "actor-in-a-box" (not needed now)
        final Inbox inbox = Inbox.create(system);

        //create test testplan and run
        Testplan myTestplan = new Testplan();
        myTestplan.testId = 0;
        myTestplan.numRuns = 1; // Per Parallel Worker
        myTestplan.parallelity = 1;
        myTestplan.path = new URL("http://www.google.com");
        myTestplan.waitBetweenMsgs = 0;
        myTestplan.waitBeforeStart = 0;
        myTestplan.connectionType = ConnectionType.HTTP;
        
        Testrun myTestrun = new Testrun();
        myTestrun.id = 0;
        myTestrun.testplan = myTestplan;
        myTestrun.subscribers.add(loadworker);
        
        //let loadworker work
        loadworker.tell(myTestrun, ActorRef.noSender());        
        
    	return redirect(routes.Application.index());
    }

}
