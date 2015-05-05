package aktors

import java.net.URL

import akka.actor.UntypedActor
import aktors.messages.Testplan.ConnectionType
import aktors.messages._
import com.mongodb.casbah.{MongoCollection, MongoClient}
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId

/**
 * Created by Patrick Robinson on 02.05.15.
 */
class DB(database : String) extends UntypedActor {
	val client = MongoClient()
	val db = client(database)
	val testruncoll = db("testrun")
	val testplancoll = db("testplan")
	val usercoll = db("user")

	def this() = this("loadgen")

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
			case getCMD : DBGetCMD => {
				getCMD.t match {
					case DBGetCMD.Type.AllPlansForUser => {
						testplancoll
							.filter("user" $eq getCMD.id)
							.foreach(planDocument => {
							getSender().tell(convertPlan(planDocument), getSelf())
						})
					}
					case DBGetCMD.Type.PlanByID => getSender().tell(getPlan(getCMD.id), getSelf())
					case DBGetCMD.Type.RunByID => getSender().tell(getRun(getCMD.id), getSelf())
					case DBGetCMD.Type.UserByID => getSender().tell(getUser(getCMD.id), getSelf())
					case DBGetCMD.Type.RunRaws => {
						val testrunobj = testruncoll.findOneByID(getCMD.id).get
						new ObjectId()
						val testrun = convertRun(testrunobj) // TODO expensive, can cut somehow?
						testrunobj.get("runs").asInstanceOf[MongoCollection].foreach(obj => {
							val raw = new LoadWorkerRaw()
							raw.testrun = testrun
							raw.start = obj.get("start").asInstanceOf[Int]
							raw.end = obj.get("end").asInstanceOf[Int]
							raw.iterOnWorker = obj.get("iter").asInstanceOf[Int]
							getSender().tell(raw, getSelf())
						})
					}
				}
			}
			case del : DBDelCMD => {
				var coll = usercoll
				del.t match {
					case DBDelCMD.Type.Plan => coll = testplancoll
					case DBDelCMD.Type.Plan => coll = testruncoll
					case DBDelCMD.Type.User => coll = usercoll
					case _ => return
				}
				coll.findAndRemove(MongoDBObject("_id" -> del.id))
			}
			case simple : String => {
				simple match {
					case "close" => {
						client.close()
						getContext.stop(getSelf)
					}
					case _ => unhandled(message)
				}
			}
			case _ => unhandled(message)
		}

		def getUser(id : ObjectId) : User = {
			// TODO cache?
			val userDocument = usercoll.findOneByID(id).get
			val userObject = new User()
			userObject.id = id
			userObject.name = userDocument.get("name").asInstanceOf[String]
			return userObject
		}

		def convertPlan(document : testplancoll.T) : Testplan = {
			// TODO use github.com/scala/async ?
			// TODO utilise currentuser, when account subsystem is implemented?
			val planObject = new Testplan()
			planObject.user = getUser(document.get("user").asInstanceOf[ObjectId])
			planObject.testId = document.get("_id").asInstanceOf[ObjectId]
			planObject.waitBeforeStart = document.get("waitBeforeStart").asInstanceOf[Int]
			planObject.waitBetweenMsgs = document.get("waitBetweenMsgs").asInstanceOf[Int]
			planObject.parallelity = document.get("parallelity").asInstanceOf[Int]
			planObject.numRuns = document.get("numRuns").asInstanceOf[Int]
			planObject.path = new URL(document.get("path").asInstanceOf[String])
			planObject.connectionType = ConnectionType.valueOf(document.get("connectionType").asInstanceOf[String])
			return planObject
		}

		def getPlan(id : ObjectId) : Testplan = convertPlan(testplancoll.findOneByID(id).get)

		def getRun(id : ObjectId) : Testrun = convertRun(testruncoll.findOneByID(id).get)

		def convertRun(runDocument : testruncoll.T) : Testrun = {
			val runObject = new Testrun()
			runObject.id = runDocument.get("_id").asInstanceOf[ObjectId]
			runObject.testplan = getPlan(runDocument.get("testPlanId").asInstanceOf[ObjectId])
			return runObject
		}
	}
}
