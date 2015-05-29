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
	def fromJSON(plan: JsObject): Testplan = {
		val result: Testplan = new Testplan
		result.setId(if(plan.\("id").isInstanceOf[JsString])
			new ObjectId(JSONHelper.JsStringToString(plan.\("id")))
		else
			new ObjectId
		)
		if(plan.\("user").isInstanceOf[JsObject]) {
			result.user = Future { User.fromJSON(plan.\("user").asInstanceOf[JsObject]) }
		}
		result.setPath(new URL(JSONHelper.JsStringToString(plan.\("path"))))
		result.setConnectionType(ConnectionType.valueOf(JSONHelper.JsStringToString(plan.\("connectionType"))))
		result.setNumRuns((plan.\("numRuns").asInstanceOf[JsNumber]).value.intValue)
		result.setParallelity((plan.\("parallelity").asInstanceOf[JsNumber]).value.intValue)
		result.setWaitBetweenMsgs(if (plan.\("waitBetweenMsgs").isInstanceOf[JsNumber]) (plan.\("waitBetweenMsgs").asInstanceOf[JsNumber]).value.intValue else 0)
		result.setWaitBeforeStart(if (plan.\("waitBeforeStart").isInstanceOf[JsNumber]) (plan.\("waitBeforeStart").asInstanceOf[JsNumber]).value.intValue else 0)
		return result
	}
}

class Testplan { // TODO FIX new blockiert
	override def hashCode: Int = getId.hashCode

	override def equals(other: Any): Boolean = other match {
		case that: Testplan => this.getId.equals(that.getId)
		case json: JsObject => new ObjectId(JSONHelper.JsStringToString((json.\("id")))).equals(this.getId)
		case _ => false
	}

	private[this] var id: ObjectId = null

	def getId: ObjectId = id

	def setId(value: ObjectId): Unit = {
	  id = value
	}

	private[this] var numRuns: Int = 0

	def getNumRuns: Int = numRuns

	def setNumRuns(value: Int): Unit = {
	  numRuns = value
	}

	private[this] var parallelity: Int = 0

	def getParallelity: Int = parallelity

	def setParallelity(value: Int): Unit = {
	  parallelity = value
	}

	private[this] var path: URL = null

	def getPath: URL = path

	def setPath(value: URL): Unit = {
	  path = value
	}

	private[this] var waitBetweenMsgs: Int = 0

	def getWaitBetweenMsgs: Int = waitBetweenMsgs

	def setWaitBetweenMsgs(value: Int): Unit = {
	  waitBetweenMsgs = value
	}

	private[this] var waitBeforeStart: Int = 0

	def getWaitBeforeStart: Int = waitBeforeStart

	def setWaitBeforeStart(value: Int): Unit = {
	  waitBeforeStart = value
	}

	private[this] var connectionType: ConnectionType = ConnectionType.HTTP

	def getConnectionType: ConnectionType = connectionType

	def setConnectionType(value: ConnectionType): Unit = {
	  connectionType = value
	}

	var user: Future[User] = Future {null}

	def getUser: User = Await.result(user, Duration(10, TimeUnit.MINUTES))

	def setUser(User: User) = user = Future { User } // TODO better
	def setUser(User: Future[User]) = user = User

	def toJSON: JsObject = toJSON(true)

	def toJSON(withUser: Boolean): JsObject = // TODO duplicate code
	if(withUser)
		Json.obj(
			"id" -> JsString(getId.toString)
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
			"id" -> JsString(getId.toString)
		,   "path" -> JsString(getPath.toString)
		,   "connectionType" -> JsString(getConnectionType.toString)
		,   "numRuns" -> JsNumber(getNumRuns)
		,   "parallelity" -> JsNumber(getParallelity)
		,   "waitBetweenMsgs" -> JsNumber(getWaitBetweenMsgs)
		,   "waitBeforeStart" -> JsNumber(getWaitBeforeStart)
		)
}