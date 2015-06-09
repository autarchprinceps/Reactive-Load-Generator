package aktors

import akka.actor.{ActorRef, UntypedActor}
import aktors.messages.{LoadWorkerRaw, DBGetCMD, Testrun}
import helper.JSONHelper

/**
 * Created by autarch on 29.05.15.
 */
private object RunLoader {
	def props(out : ActorRef, db : ActorRef) : RunLoader = new RunLoader(out, db)
}

private class RunLoader(out : ActorRef, db: ActorRef) extends UntypedActor {
	@throws[Exception](classOf[Exception])
	override def onReceive(message: Any): Unit = message match {
		case raw : LoadWorkerRaw => out.tell(JSONHelper.objectResponse("raw", (message.asInstanceOf[LoadWorkerRaw]).toJSON(false)).toString, getSelf)
		case testrun : Testrun => {
			out.tell(JSONHelper.objectResponse("testrun", testrun.toJSON).toString, getSelf)
			val dbGetCMD: DBGetCMD = new DBGetCMD
			dbGetCMD.t = DBGetCMD.Type.RunRaws
			dbGetCMD.id = testrun.getID
			db.tell(dbGetCMD, getSelf)
		}
		case _ => unhandled(message)
	}
}
