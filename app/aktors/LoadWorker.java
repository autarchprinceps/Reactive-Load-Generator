package aktors;

import akka.actor.UntypedActor;
import aktors.messages.InitLoadWorker;
import aktors.messages.LoadWorkerRaw;
import aktors.messages.Testplan;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class LoadWorker extends UntypedActor {
    @Override
    public void onReceive(Object message) {
        if(message instanceof InitLoadWorker) {
            InitLoadWorker init = (InitLoadWorker)message;
            Testplan plan = init.testplan;
            // TODO CHECK: separate thread needed here?
            if(plan.waitBeforeStart > 0) {
                try {
                    Thread.sleep(plan.waitBeforeStart);
                } catch (InterruptedException e) {
                    // TODO: WTF?
                }
            }
            for(int i = 0; i < plan.numRuns; i++) {
                long time = System.currentTimeMillis();
                // TODO: actually perform test
                time = System.currentTimeMillis() - time;
                LoadWorkerRaw msg = new LoadWorkerRaw();
                msg.testId = plan.testId;
                msg.time = time;
                init.subscribers.parallelStream().forEach((actorRef -> {
                    actorRef.tell(msg, getSelf());
                }));
                if(plan.waitBetweenMsgs > 0) {
                    try {
                        Thread.sleep(plan.waitBetweenMsgs);
                    } catch (InterruptedException e) {
                        // TODO: WTF?
                    }
                }
            }
        } else {
            unhandled(message);
        }
    }
}
