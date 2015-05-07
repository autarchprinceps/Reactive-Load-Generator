package aktors;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.io.InputStreamReader;





import akka.actor.UntypedActor;

import java.net.URL;

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
                    LoadWorkerRaw msg = new LoadWorkerRaw(init, i, System.currentTimeMillis(), 0);
                    
                    // HTTP GET Request
                    try {
                    	HttpURLConnection connection = null;
                    	URL url = plan.path;

                    	connection = (HttpURLConnection)url.openConnection();

                    	connection.setRequestMethod("GET");

                    	int responseCode = connection.getResponseCode();
                    	System.out.println("\nSending 'GET' request to URL : " + url);
                    	System.out.println("Response Code : " + responseCode);

                    	BufferedReader in = new BufferedReader(
                    			new InputStreamReader(connection.getInputStream()));
                    	String inputLine;
                    	StringBuffer response = new StringBuffer();

                    	while ((inputLine = in.readLine()) != null) {
                    		response.append(inputLine);
                    	}
                    	in.close();

                    	//print result
                    	System.out.println(response.toString());
                    } catch (Exception e1) {
                    	// TODO Auto-generated catch block
                    	e1.printStackTrace();
                    }

                    
                    
                    
                    System.out.println("Loadworker performing the actual test");
                    msg.end_(System.currentTimeMillis());
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
                getContext().stop(getSelf()); // TODO works? else self().tell(PoisonPill.getInstance(), self());
            }
        } else {
            unhandled(message);
        }
    }

	@Override
    public void postStop() throws Exception {
	    t.stop();
    }
}
