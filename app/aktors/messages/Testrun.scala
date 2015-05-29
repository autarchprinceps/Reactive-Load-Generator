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
		id = new ObjectId(JSONHelper.JsStringToString(run.\("id")))
	,   testplan = Future {if(run.\("testplan").isInstanceOf[JsObject]) Testplan.fromJSON(run.\("testplan").asInstanceOf[JsObject]) else null}
	)
}

class Testrun(id: ObjectId = new ObjectId, subscribers: List[ActorRef] = new util.ArrayList[ActorRef](), testplan: Future[Testplan] = null) {
	var _testplan: Future[Testplan] = testplan

	def getID: ObjectId = id
	def getTestplan: Testplan = Await.result(testplan, Duration(10, TimeUnit.MINUTES))
	def setTestplan(plan: Testplan) = _testplan = Future {plan} // TODO better?
	def setTestplan(plan: Future[Testplan]) = _testplan = plan
	def getSubscribers: List[ActorRef] = subscribers

	def toJSON: JsObject = toJSON(true)

	def toJSON(withPlan: Boolean): JsObject =
		if(withPlan) // TODO better?
			Json.obj(
				"id" -> JsString(id.toString)
			,   "testplan" -> getTestplan.toJSON
			)
		else
			Json.obj("id" -> JsString(id.toString))

	override def hashCode: Int = id.hashCode

	override def equals(other: Any): Boolean = other match {
		case that: Testrun => this.id.equals(that.id)
		case json: JsObject => new ObjectId(JSONHelper.JsStringToString((json.\("id")))).equals(this.id)
		case _ => false
	}
}