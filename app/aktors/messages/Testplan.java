package aktors.messages;

import java.net.URL;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class Testplan {
    public int testId;
    public int numRuns;
    public URL path;
    public int waitBetweenMsgs = 0;
    public int waitBeforeStart = 0;
}
