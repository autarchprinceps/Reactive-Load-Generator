package aktors;

import akka.actor.UntypedActor;
import aktors.messages.Testrun;
import aktors.messages.LoadWorkerRaw;
import aktors.messages.Testplan;
import aktors.messages.WorkerCMD;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class LoadWorker extends UntypedActor {
    private Thread t;
    @Override
    public void onReceive(Object message) {
        if(message instanceof Testrun) {
            Testrun init = (Testrun)message;
            Testplan plan = init.testplan;
            t = new Thread(() -> {
                if (plan.waitBeforeStart > 0) {
                    try {
                        Thread.sleep(plan.waitBeforeStart);
                    } catch (InterruptedException e) {
                        // TODO: WTF?
                    }
                }
                for (int i = 0; i < plan.numRuns; i++) {
                    LoadWorkerRaw msg = new LoadWorkerRaw();
                    msg.testrun = init;
                    msg.iterOnWorker = i;
                    msg.start = System.currentTimeMillis();
                    // TODO: actually perform test
                    msg.end = System.currentTimeMillis();
                    init.subscribers.parallelStream().forEach((actorRef -> actorRef.tell(msg, getSelf())));
                    if (plan.waitBetweenMsgs > 0) {
                        try {
                            Thread.sleep(plan.waitBetweenMsgs);
                        } catch (InterruptedException e) {
                            // TODO: WTF?
                        }
                    }
                }
            });
            t.start();
        } else if(message instanceof WorkerCMD) {
            WorkerCMD cmd = (WorkerCMD)message;
            if(cmd == WorkerCMD.Stop) {
                t.stop();
                // TODO selfdestruct
            }
        } else {
            unhandled(message);
        }
    }
}
