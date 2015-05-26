package aktors.messages

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
	def fromJSON(run: JsObject): Testrun = {
		val result: Testrun = new Testrun
		result.id = new ObjectId(JSONHelper.JsStringToString(run.\("id")))
		result.testplan = Future {if(run.\("testplan").isInstanceOf[JsObject]) Testplan.fromJSON(run.\("testplan").asInstanceOf[JsObject]) else null}
		return result
	}
}

class Testrun {
	var id: ObjectId = null
	var testplan: Future[Testplan] = null
	var subscribers: List[ActorRef] = null

	def getID: ObjectId = id
	def setID(I:ObjectId) = id = I
	def getTestplan: Testplan = Await.result(testplan, Duration(10, TimeUnit.MINUTES))
	def setTestplan(plan: Testplan) = testplan = Future {plan} // TODO better?
	def setTestplan(plan: Future[Testplan]) = testplan = plan
	def getSubscribers: List[ActorRef] = subscribers
	def setSubscribers(list : List[ActorRef]) = subscribers = list

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