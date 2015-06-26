package aktors.messages

import java.util
import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import helper.JSONHelper
import org.bson.types.ObjectId
import play.api.libs.json._
import java.net.MalformedURLException
import java.util.List

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Patrick Robinson on 20.04.15.
 */
object Testrun {
	@throws(classOf[MalformedURLException])
	def fromJSON(run: JsObject): Testrun = new Testrun(
		ID = new ObjectId(JSONHelper.JsStringToString(run.\("id")))
	,   Testplan = Future {if(run.\("testplan").isInstanceOf[JsObject]) Testplan.fromJSON(run.\("testplan").asInstanceOf[JsObject]) else null}
	)
}

class Testrun(ID: ObjectId = new ObjectId, Subscribers: List[ActorRef] = new util.ArrayList[ActorRef](), Testplan: Future[Testplan] = null) {
	def this(ID:ObjectId, Subscribers: List[ActorRef], Testplan:Testplan) = this(ID, Subscribers, Future {Testplan})

	var _testplan: Future[Testplan] = Testplan

	def getID: ObjectId = ID
	def getTestplan: Testplan = Await.result(Testplan, Duration(10, TimeUnit.MINUTES))
	def setTestplan(plan: Testplan) = _testplan = Future {plan}
	def setTestplan(plan: Future[Testplan]) = _testplan = plan
	def getSubscribers: List[ActorRef] = Subscribers

	def toJSON: JsObject = toJSON(true)
	def toJSON(withPlan: Boolean): JsObject =
		if(withPlan)
			Json.obj(
				"id" -> JsString(ID.toString)
			,   "testplan" -> getTestplan.toJSON
			)
		else
			Json.obj("id" -> JsString(ID.toString))

	override def hashCode: Int = ID.hashCode

	override def equals(other: Any): Boolean = other match {
		case that: Testrun => getID.equals(that.getID)
		case json: JsObject => new ObjectId(JSONHelper.JsStringToString((json.\("id")))).equals(this.ID)
		case _ => false
	}
}