package aktors

import akka.actor.{ActorPath, ActorRef, UntypedActor}
import aktors.messages.{Testrun, LoadWorkerRaw}
import helper.JSONHelper

/**
 * Created by Patrick Robinson on 29.05.15.
 */
private object RunnerConnector {
	def props(out : ActorRef) : RunnerConnector = new RunnerConnector(out)
}

private class RunnerConnector(out : ActorRef) extends UntypedActor {
	@throws[Exception](classOf[Exception])
	override def onReceive(message: Any): Unit = message match {
		case raw : LoadWorkerRaw => out.tell(JSONHelper.objectResponse("raw", raw.toJSON(false)).toString, getSelf)
		case run : Testrun => out.tell(JSONHelper.objectResponse("testrun", run.toJSON(true)).toString, getSelf) // TODO apply & test (from runstart to testrun)
		case _ => unhandled(message)
	}
}
