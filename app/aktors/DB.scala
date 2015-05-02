package aktors

import akka.actor.UntypedActor
import aktors.messages.{Testplan, Testrun, LoadWorkerRaw}
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject

/**
 * Created by Patrick Robinson on 02.05.15.
 */
class DB extends UntypedActor {
	val db = MongoClient()("loadgen")
	val testruncoll = db("testrun")
	val testplancoll = db("testplan")
	val usercoll = db("user")

	@throws[Exception](classOf[Exception])
	override def onReceive(message: Any): Unit = {
		message match {
			case workerraw : LoadWorkerRaw => {
				val query = MongoDBObject("_id" -> workerraw.testrun.id)
				val run = MongoDBObject("start" -> workerraw.start) ++ ("end" -> workerraw.end) ++ ("iter" -> workerraw.iterOnWorker)
				val update = $push("runs" -> run)
				testruncoll.update(query, update)
			}
			case trun : Testrun => testruncoll.insert(MongoDBObject("_id" -> trun.id) ++ ("testPlanId" -> trun.testplan.testId))
			case tplan : Testplan => testplancoll.insert(
				MongoDBObject("_id" -> tplan.testId)
			++	("numRuns" -> tplan.numRuns)
			++	("parallelity" -> tplan.parallelity)
			++	("path" -> tplan.path.toString)
			++	("waitBetweenMsgs" -> tplan.waitBetweenMsgs)
			++	("waitBeforeStart" -> tplan.waitBeforeStart)
			++	("connectionType" -> tplan.connectionType.toString)
			)
			case other => unhandled(message)
		}
	}
}
