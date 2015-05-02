package aktors.messages;

import java.net.URL;

/**
 * Created by Patrick Robinson on 20.04.15.
 */
public class Testplan {
    public enum ConnectionType {
        HTTP, TCP, UDP, WebSocket
    }

    public int testId;
    public int numRuns; // Per Parallel Worker
    public int parallelity;
    public URL path;
    public int waitBetweenMsgs = 0;
    public int waitBeforeStart = 0;
    public ConnectionType connectionType = ConnectionType.HTTP;
    public User user;
}
