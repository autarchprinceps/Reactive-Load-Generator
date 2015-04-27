package aktors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import aktors.messages.RunnerCMD;
import aktors.messages.Testplan;
import aktors.messages.Testrun;
import aktors.messages.WorkerCMD;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick Robinson on 22.04.15.
 */
public class LoadRunner extends UntypedActor {
    private ActorRef db;
    private List<ActorRef> workers;
    private Testrun testrun;

    @Override
    public void onReceive(Object message) {
        
        /*
        if(message instanceof Testplan) {
            testrun = new Testrun();
            testrun.id = 0; // TODO replace 0
            testrun.testplan = (Testplan)message;
            final ActorSystem system = ActorSystem.create();
            db = system.actorOf(Props.create(DBInterface.class));
            testrun.subscribers.add(db);
            db.tell(testrun, getSelf());

            workers = new ArrayList<>(testrun.testplan.parallelity);
            for(int i = 0; i < testrun.testplan.parallelity; i++) {
                workers.add(system.actorOf(Props.create(LoadWorker.class)));
            }
        } else if(message instanceof RunnerCMD) {
            RunnerCMD cmd = (RunnerCMD)message;
            switch(message) {
                case Start:
                    workers.parallelStream().forEach((x) -> x.tell(testrun, getSelf()));
                    break;
                case Stop:
                    workers.parallelStream().forEach((x) -> x.tell(WorkerCMD.Stop, getSelf()));
                    // TODO selfdestruct
                    break;
            }
        }
        */
    }
}
