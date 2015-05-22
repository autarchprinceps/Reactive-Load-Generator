package aktors

import java.net.URL

import akka.actor.UntypedActor
import aktors.messages.Testplan.ConnectionType
import aktors.messages._
import com.mongodb.casbah.Imports._

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
				testruncoll.update(query, update) // TODO imperformant
			}
			case trun : Testrun => testruncoll.insert(MongoDBObject("_id" -> trun.id, "testPlanId" -> trun.testplan.id))
			case tplan : Testplan => testplancoll.insert(MongoDBObject(
				"_id" -> tplan.id
			,	"numRuns" -> tplan.numRuns
			,	"parallelity" -> tplan.parallelity
			,	"path" -> tplan.path.toString()
			,	"waitBetweenMsgs" -> tplan.waitBetweenMsgs
			,	"waitBeforeStart" -> tplan.waitBeforeStart
			,	"connectionType" -> tplan.connectionType.toString()
			,	"user" -> tplan.user.id
			))
			case user : User => usercoll.insert(MongoDBObject( // TODO what if user exists already?
				"_id" -> user.id
			,	"name" -> user.name
			,   "password" -> user.getPassword
			))
			case getCMD : DBGetCMD => {
				getCMD.t match {
					case DBGetCMD.Type.AllPlansForUser => {
						testplancoll.find("user" $eq getCMD.id).foreach(planDocument => getSender().tell(convertPlan(planDocument), getSelf()))
					}
					case DBGetCMD.Type.AllRunsForPlan => {
						testruncoll.find("testPlanId" $eq getCMD.id).foreach(runDocument => getSender().tell(convertRun(runDocument), getSelf()))
					}
					case DBGetCMD.Type.PlanByID => getSender().tell(getPlan(getCMD.id), getSelf())
					case DBGetCMD.Type.RunByID => getSender().tell(getRun(getCMD.id), getSelf())
					case DBGetCMD.Type.UserByID => getSender().tell(getUser(getCMD.id), getSelf())
					case DBGetCMD.Type.RunRaws => {
						val testrunobj = testruncoll.findOneByID(getCMD.id).get
						val testrun = convertRun(testrunobj) // TODO expensive, can cut somehow?
						testrunobj.getAs[MongoDBList]("runs").get.foreach(obj => {
							val raw = obj.asInstanceOf[BasicDBObject]
							getSender().tell(new LoadWorkerRaw(testrun, raw.getAs[Int]("iter").get, raw.getAs[Long]("start").get, raw.getAs[Long]("end").get), getSelf())
						})
					}
				}
			}
			case query : DBQuery => {
				query.t match {
					case DBQuery.Type.Login => {
						val result = usercoll.findOne(MongoDBObject("name" -> query.terms.get("name"))).get
						val user = new User(
							result.getAs[ObjectId]("_id").get
						,   result.getAs[String]("name").get
						,   result.getAs[String]("password").get
						)
						query.flag = user.check(query.terms.get("password"))
						if(query.flag) query.result = user
						getSender.tell(query, getSelf)
					}
					case _ => return
				}
			}
			case del : DBDelCMD => {
				var coll = usercoll
				del.t match {
					case DBDelCMD.Type.Plan => coll = testplancoll
					case DBDelCMD.Type.Run => coll = testruncoll
					case DBDelCMD.Type.User => coll = usercoll
					case DBDelCMD.Type.DB => {
						testruncoll.drop
						testplancoll.drop
						usercoll.drop
						return
					}
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
			return new User(
				id
			,   userDocument.getAs[String]("name").get
			,	userDocument.getAs[String]("password").get
			)
		}

		def convertPlan(document : testplancoll.T) : Testplan = {
			// TODO use github.com/scala/async ?
			// TODO utilise currentuser, when account subsystem is implemented?
			val planObject = new Testplan()
			planObject.user = getUser(document.getAs[ObjectId]("user").get)
			planObject.id = document.getAs[ObjectId]("_id").get
			planObject.waitBeforeStart = document.getAs[Int]("waitBeforeStart").get
			planObject.waitBetweenMsgs = document.getAs[Int]("waitBetweenMsgs").get
			planObject.parallelity = document.getAs[Int]("parallelity").get
			planObject.numRuns = document.getAs[Int]("numRuns").get
			planObject.path = new URL(document.getAs[String]("path").get)
			planObject.connectionType = ConnectionType.valueOf(document.getAs[String]("connectionType").get)
			return planObject
		}

		def getPlan(id : ObjectId) : Testplan = convertPlan(testplancoll.findOneByID(id).get)

		def getRun(id : ObjectId) : Testrun = convertRun(testruncoll.findOneByID(id).get)

		def convertRun(runDocument : testruncoll.T) : Testrun = {
			val runObject = new Testrun()
			runObject.id = runDocument.getAs[ObjectId]("_id").get
			runObject.testplan = getPlan(runDocument.getAs[ObjectId]("testPlanId").get)
			return runObject
		}
	}
}
