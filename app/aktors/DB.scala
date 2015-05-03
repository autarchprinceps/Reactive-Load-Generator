package aktors

import java.net.URL

import akka.actor.UntypedActor
import aktors.messages.Testplan.ConnectionType
import aktors.messages._
import com.mongodb.DBObject
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
				val run = MongoDBObject("start" -> workerraw.start, "end" -> workerraw.end, "iter" -> workerraw.iterOnWorker)
				val update = $push("runs" -> run)
				testruncoll.update(query, update)
			}
			case trun : Testrun => testruncoll.insert(MongoDBObject("_id" -> trun.id, "testPlanId" -> trun.testplan.testId))
			case tplan : Testplan => testplancoll.insert(MongoDBObject(
				"_id" -> tplan.testId
			,	"numRuns" -> tplan.numRuns
			,	"parallelity" -> tplan.parallelity
			,	"path" -> tplan.path.toString()
			,	"waitBetweenMsgs" -> tplan.waitBetweenMsgs
			,	"waitBeforeStart" -> tplan.waitBeforeStart
			,	"connectionType" -> tplan.connectionType.toString()
			,	"user" -> tplan.user.id
			))
			case user : User => usercoll.insert(MongoDBObject(
				"_id" -> user.id
			,	"name" -> user.name
			))
			case get : DBGetCMD => {
				get.t match {
					case DBGetCMD.Type.AllPlansForUser => {
						testplancoll.filter("user" $eq get.id).foreach(planDocument => {
							getSender().tell(convertPlan(planDocument), getSelf())
						})
					}
					case DBGetCMD.Type.PlanByID => getSender().tell(getPlan(get.id), getSelf())
					case DBGetCMD.Type.RunByID => getSender().tell(getRun(get.id), getSelf())
					case DBGetCMD.Type.UserByID => getSender().tell(getUser(get.id), getSelf())
				}
			}
			case _ => unhandled(message)
		}

		def getUser(id : Int) : User = {
			// TODO cache?
			val userDocument = usercoll.findOneByID(id)
			val userObject = new User()
			userObject.id = id
			userObject.name = userDocument.get.get("name").asInstanceOf[String]
			return userObject
		}

		def convertPlan(document : DBObject) : Testplan = {
			// TODO use github.com/scala/async ?
			// TODO utilise currentuser, when account subsystem is implemented?
			val planObject = new Testplan()
			planObject.user = getUser(document.get("user").asInstanceOf[Int])
			planObject.testId = document.get("_id").asInstanceOf[Int]
			planObject.waitBeforeStart = document.get("waitBeforeStart").asInstanceOf[Int]
			planObject.waitBetweenMsgs = document.get("waitBetweenMsgs").asInstanceOf[Int]
			planObject.parallelity = document.get("parallelity").asInstanceOf[Int]
			planObject.numRuns = document.get("numRuns").asInstanceOf[Int]
			planObject.path = new URL(document.get("path").asInstanceOf[String])
			planObject.connectionType = ConnectionType.valueOf(document.get("connectionType").asInstanceOf[String])
			return planObject
		}

		def getPlan(id : Int) : Testplan = convertPlan(testplancoll.findOneByID(id).get)

		def getRun(id : Int) : Testrun = {
			// TODO use github.com/scala/async .. see getPlan
			val runDocument = testruncoll.findOneByID(id)
			val runObject = new Testrun()
			runObject.id = id
			runObject.testplan = getPlan(runDocument.get.get("testPlanId").asInstanceOf[Int])
			return runObject
		}
	}
}
