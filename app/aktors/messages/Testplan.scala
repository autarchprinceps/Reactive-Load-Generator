package aktors.messages

import java.util.concurrent.TimeUnit

import helper.JSONHelper
import org.bson.types.ObjectId
import play.api.libs.json._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import java.net.MalformedURLException
import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Patrick Robinson on 20.04.15.
 */
object Testplan {
	@throws(classOf[MalformedURLException])
	def fromJSON(plan: JsObject): Testplan = new Testplan(
		ID = if(plan.\("id").isInstanceOf[JsString]) new ObjectId(JSONHelper.JsStringToString(plan.\("id"))) else new ObjectId,
		Path = new URL(JSONHelper.JsStringToString(plan.\("path"))),
		ConType =ConnectionType.valueOf(JSONHelper.JsStringToString(plan.\("connectionType"))),
		NumRuns = (plan.\("numRuns").asInstanceOf[JsNumber]).value.intValue,
		Parallelity = (plan.\("parallelity").asInstanceOf[JsNumber]).value.intValue,
		WaitBetweenMsgs = if (plan.\("waitBetweenMsgs").isInstanceOf[JsNumber]) (plan.\("waitBetweenMsgs").asInstanceOf[JsNumber]).value.intValue else 0,
		WaitBeforeStart = if (plan.\("waitBeforeStart").isInstanceOf[JsNumber]) (plan.\("waitBeforeStart").asInstanceOf[JsNumber]).value.intValue else 0,
		User = Future { if(plan.\("user").isInstanceOf[JsObject]) User.fromJSON(plan.\("user").asInstanceOf[JsObject]) else null }
	)
}

class Testplan(
	ID : ObjectId = new ObjectId,
	NumRuns : Int,
	Parallelity : Int = 1,
	Path : URL,
	WaitBetweenMsgs : Int = 0,
    WaitBeforeStart : Int = 0,
	ConType : ConnectionType = ConnectionType.HTTP,
	User : Future[User] = Future {null}
) {
	override def hashCode: Int = getID.hashCode

	override def equals(other: Any): Boolean = other match {
		case that: Testplan => this.getID.equals(that.getID)
		case json: JsObject => new ObjectId(JSONHelper.JsStringToString((json.\("id")))).equals(this.getID)
		case _ => false
	}

	def getID: ObjectId = ID
	def getNumRuns: Int = NumRuns
	def getParallelity: Int = Parallelity
	def getPath: URL = Path
	def getWaitBetweenMsgs: Int = WaitBetweenMsgs
	def getWaitBeforeStart: Int = WaitBeforeStart
	def getConnectionType: ConnectionType = ConType

	var user: Future[User] = User
	def getUser: User = Await.result(user, Duration(10, TimeUnit.MINUTES))
	def setUser(User: User) = user = Future { User } // TODO better
	def setUser(User: Future[User]) = user = User

	def toJSON: JsObject = toJSON(true)
	def toJSON(withUser: Boolean): JsObject = // TODO duplicate code
	if(withUser)
		Json.obj(
			"id" -> JsString(getID.toString)
		,   "path" -> JsString(getPath.toString)
		,	"user" -> getUser.toJSON
		,   "connectionType" -> JsString(getConnectionType.toString)
		,   "numRuns" -> JsNumber(getNumRuns)
		,   "parallelity" -> JsNumber(getParallelity)
		,   "waitBetweenMsgs" -> JsNumber(getWaitBetweenMsgs)
		,   "waitBeforeStart" -> JsNumber(getWaitBeforeStart)
		)
	else
		Json.obj(
			"id" -> JsString(getID.toString)
		,   "path" -> JsString(getPath.toString)
		,   "connectionType" -> JsString(getConnectionType.toString)
		,   "numRuns" -> JsNumber(getNumRuns)
		,   "parallelity" -> JsNumber(getParallelity)
		,   "waitBetweenMsgs" -> JsNumber(getWaitBetweenMsgs)
		,   "waitBeforeStart" -> JsNumber(getWaitBeforeStart)
		)
}