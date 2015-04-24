package aktors.messages;

import akka.actor.ActorRef;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class Testrun {
    public int id;
    public Testplan testplan;
    public List<ActorRef> subscribers = new ArrayList<>();
}