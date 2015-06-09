package aktors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import aktors.messages.RunnerCMD;
import aktors.messages.Testplan;
import aktors.messages.Testrun;
import aktors.messages.WorkerCMD;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick Robinson on 22.04.15.
 */
public class LoadRunner extends UntypedActor {
	public static LoadRunner props(ActorSystem as, Testplan testplan, ActorRef db, ActorRef ui) {
		return new LoadRunner(as, testplan, db, ui);
	}

	public LoadRunner(ActorSystem as, Testplan testplan, ActorRef db, ActorRef ui) {
		this.as = as;
		subscribers = new ArrayList<>(2);
		subscribers.add(db);
		subscribers.add(ui);

        testrun = new Testrun(new ObjectId(), subscribers, testplan);
		System.out.println("DEBUG LoadRunner testrun: " + testrun.toJSON(true));
		subscribers.parallelStream().forEach(actorRef -> actorRef.tell(testrun, getSelf()));

        workers = new ArrayList<>(testrun.getTestplan().getParallelity());
        for(int i = 0; i < testrun.getTestplan().getParallelity(); i++) {
            workers.add(as.actorOf(Props.create(LoadWorker.class)));
        }
	}

	private final ActorSystem as;
    private List<ActorRef> workers;
    private Testrun testrun;
	private List<ActorRef> subscribers;

    @Override
    public void onReceive(Object message) {
        if(message instanceof RunnerCMD) {
            RunnerCMD cmd = (RunnerCMD)message;
            switch(cmd) {
                case Start:
                    workers.parallelStream().forEach((x) -> x.tell(testrun, getSelf()));
                    break;
                case Stop:
                    workers.parallelStream().forEach((x) -> x.tell(WorkerCMD.Stop, getSelf()));
                    // DEBUG deadletters getContext().stop(getSelf());
                    break;
            }
        } else {
            unhandled(message);
        }
    }

    @Override
    public void postStop() throws Exception {
        workers.parallelStream().forEach((x) -> x.tell(WorkerCMD.Stop, getSelf()));
    }
}
