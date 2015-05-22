package aktors.messages

import java.util.concurrent.TimeUnit

import helper.JSONHelper
import org.bson.types.ObjectId
import play.api.libs.json._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by Patrick Robinson on 20.04.15.
 */
object Testplan {
	@throws(classOf[MalformedURLException])
	def fromJSON(plan: JsObject): Testplan = {
		val result: Testplan = new Testplan
		result.id = if(plan.\("id").isInstanceOf[JsString])
			new ObjectId(JSONHelper.JsStringToString(plan.\("id")))
		else
			new ObjectId
		if (plan.\("user").isInstanceOf[JsObject]) {
			result._user = Future { User.fromJSON(plan.\("user").asInstanceOf[JsObject]) }
		}
		result.path = new URL(JSONHelper.JsStringToString(plan.\("path")))
		result.connectionType = ConnectionType.valueOf(JSONHelper.JsStringToString(plan.\("connectionType")))
		result.numRuns = (plan.\("numRuns").asInstanceOf[JsNumber]).value.intValue
		result.parallelity = (plan.\("parallelity").asInstanceOf[JsNumber]).value.intValue
		result.waitBetweenMsgs = if (plan.\("waitBetweenMsgs").isInstanceOf[JsNumber]) (plan.\("waitBetweenMsgs").asInstanceOf[JsNumber]).value.intValue else 0
		result.waitBeforeStart = if (plan.\("waitBeforeStart").isInstanceOf[JsNumber]) (plan.\("waitBeforeStart").asInstanceOf[JsNumber]).value.intValue else 0
		return result
	}
}

class Testplan {
	override def hashCode: Int = id.hashCode

	override def equals(other: Any): Boolean = other match {
		case that: Testplan => this.id.equals(that.id)
		case json: JsObject => new ObjectId(JSONHelper.JsStringToString((json.\("id")))).equals(this.id)
		case _ => false
	}

	private[this] var _id: ObjectId = null

	def id: ObjectId = _id

	def id_(value: ObjectId): Unit = {
	  _id = value
	}

	private[this] var _numRuns: Int = 0

	def numRuns: Int = _numRuns

	def numRuns_(value: Int): Unit = {
	  _numRuns = value
	}

	private[this] var _parallelity: Int = 0

	def parallelity: Int = _parallelity

	def parallelity_(value: Int): Unit = {
	  _parallelity = value
	}

	private[this] var _path: URL = null

	def path: URL = _path

	def path_(value: URL): Unit = {
	  _path = value
	}

	private[this] var _waitBetweenMsgs: Int = 0

	def waitBetweenMsgs: Int = _waitBetweenMsgs

	def waitBetweenMsgs_(value: Int): Unit = {
	  _waitBetweenMsgs = value
	}

	private[this] var _waitBeforeStart: Int = 0

	def waitBeforeStart: Int = _waitBeforeStart

	def waitBeforeStart_(value: Int): Unit = {
	  _waitBeforeStart = value
	}

	private[this] var _connectionType: ConnectionType = ConnectionType.HTTP

	def connectionType: ConnectionType = _connectionType

	def connectionType_(value: ConnectionType): Unit = {
	  _connectionType = value
	}

	var _user: Future[User] = null

	def user: User = Await.result(_user, Duration(10, TimeUnit.MINUTES))

	def user_(user : User) = _user = Future { user } // TODO better

	def toJSON: JsObject = toJSON(true)

	def toJSON(withUser: Boolean): JsObject = // TODO duplicate code
	if(withUser)
		Json.obj(
			"id" -> JsString(id.toString)
		,   "path" -> JsString(path.toString)
		,	"user" -> user.toJSON
		,   "connectionType" -> JsString(connectionType.toString)
		,   "numRuns" -> JsNumber(numRuns)
		,   "parallelity" -> JsNumber(parallelity)
		,   "waitBetweenMsgs" -> JsNumber(waitBetweenMsgs)
		,   "waitBeforeStart" -> JsNumber(waitBeforeStart)
		)
	else
		Json.obj(
			"id" -> JsString(id.toString)
		,   "path" -> JsString(path.toString)
		,   "connectionType" -> JsString(connectionType.toString)
		,   "numRuns" -> JsNumber(numRuns)
		,   "parallelity" -> JsNumber(parallelity)
		,   "waitBetweenMsgs" -> JsNumber(waitBetweenMsgs)
		,   "waitBeforeStart" -> JsNumber(waitBeforeStart)
		)
}