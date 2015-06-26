package aktors;

import java.io.BufferedReader;
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
            Testplan plan = init.getTestplan();
            t = new Thread(() -> {
                long firstStart = System.currentTimeMillis();
                if (plan.getWaitBeforeStart() > 0) {
                    try {
                        Thread.sleep(plan.getWaitBeforeStart());
                    } catch (InterruptedException ignored) {
                    }
                }
                for (int i = 0; i < plan.getNumRuns(); i++) {
                    long start = System.currentTimeMillis() - firstStart;
                    // HTTP GET Request
                    try {
                    	HttpURLConnection connection = null;
                    	URL url = plan.getPath();
                    	connection = (HttpURLConnection)url.openConnection();
                    	connection.setRequestMethod("GET");
                        BufferedReader in = new BufferedReader(
                    			new InputStreamReader(connection.getInputStream()));
                    	String inputLine;
                    	StringBuffer response = new StringBuffer();

                    	while ((inputLine = in.readLine()) != null) {
                    		response.append(inputLine);
                    	}
                    	in.close();
                    } catch (Exception e1) {
                    	// TODO Auto-generated catch block
                    	e1.printStackTrace();
                    }
	                LoadWorkerRaw msg = new LoadWorkerRaw(init, i, start, System.currentTimeMillis() - firstStart);

                    init.getSubscribers().parallelStream().forEach((actorRef -> actorRef.tell(msg, getSelf())));
                    if (plan.getWaitBetweenMsgs() > 0) try {
                        Thread.sleep(plan.getWaitBetweenMsgs());
                    } catch (InterruptedException ignored) {
                    }
                }
            });
            t.start();
        } else if(message instanceof WorkerCMD) {
            WorkerCMD cmd = (WorkerCMD)message;
            if(cmd == WorkerCMD.Stop) {
                t.stop();
                getContext().stop(getSelf());
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
