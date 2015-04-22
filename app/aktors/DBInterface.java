package aktors;

import akka.actor.UntypedActor;
import aktors.messages.LoadWorkerRaw;
import aktors.messages.Testplan;
import aktors.messages.Testrun;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class DBInterface extends UntypedActor {
    // TODO: include MongoDB Driver
    @Override
    public void onReceive(Object message) {
        if(message instanceof LoadWorkerRaw) {
            LoadWorkerRaw raw = (LoadWorkerRaw)message;
            // db.testrun.update({id:},{$push:{runs:{start:1,end:2}}},{multi:false})
        } else if(message instanceof Testrun) {
            Testrun run = (Testrun)message;
            // db.testrun.insert({id:run.id, plan:run.testplan.id, runs:[]})
        } else if(message instanceof Testplan) {
            Testplan plan = (Testplan)message;
            // db.testplan.insert({id:plan.id, ..})
        } else {
            unhandle(message);
        }
    }

}
