package aktors;

import akka.actor.UntypedActor;
import aktors.messages.LoadWorkerRaw;
import aktors.messages.Testplan;
import aktors.messages.Testrun;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.casbah.commons.MongoDBObject;

import java.net.UnknownHostException;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class DBInterface extends UntypedActor {
    private DB db;
    private DBCollection testruncoll;
    private DBCollection testplancoll;
    private DBCollection usercoll;

    public DBInterface() {
        try {
            db = new MongoClient().getDB("loadgen");
            testruncoll = db.getCollection("testrun");
            testplancoll = db.getCollection("testplan");
            usercoll = db.getCollection("user");
        } catch(UnknownHostException ex) {
            ex.printStackTrace();
        }
    }

    // TODO: include MongoDB Driver
    @Override
    public void onReceive(Object message) {
        if(message instanceof LoadWorkerRaw) {
            LoadWorkerRaw raw = (LoadWorkerRaw)message;
            MongoDBObject query = new MongoDBObject();
            MongoDBObject change = new MongoDBObject();
            testruncoll.update(query, change);
            // db.testrun.update({id:},{$push:{runs:{start:1,end:2}}},{multi:false})
        } else if(message instanceof Testrun) {
            Testrun run = (Testrun)message;
            // db.testrun.insert({id:run.id, plan:run.testplan.id, runs:[]})
        } else if(message instanceof Testplan) {
            Testplan plan = (Testplan)message;
            // db.testplan.insert({id:plan.id, ..})
        } else {
            unhandled(message);
        }
    }

}
