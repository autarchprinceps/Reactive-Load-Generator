package aktors.messages

import java.util.concurrent.TimeUnit

import play.api.libs.json._
import java.net.MalformedURLException

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by Patrick Robinson on 20.04.15.
 */
object LoadWorkerRaw {
	@throws(classOf[MalformedURLException])
	def fromJSON(raw: JsObject): LoadWorkerRaw = new LoadWorkerRaw(
		Future {if(raw.\("testrun").isInstanceOf[JsObject]) Testrun.fromJSON(raw.\("testrun").asInstanceOf[JsObject]) else null}
	,	raw.\("iterOnWorker").asInstanceOf[JsNumber].value.intValue
	,   raw.\("start").asInstanceOf[JsNumber].value.intValue
	,   raw.\("end").asInstanceOf[JsNumber].value.intValue
	)
}

class LoadWorkerRaw(Testrun: Future[Testrun], IterOnWorker : Int, StartTime : Long, EndTime : Long) {
	def getTestrun: Testrun = Await.result(Testrun, Duration(10, TimeUnit.MINUTES))
	def getIterOnWorker: Int = IterOnWorker
	def getStart: Long = StartTime
	def getEnd: Long = EndTime

	def this(Testrun: Testrun, IterOnWorker : Int, StartTime : Long, EndTime : Long) = this(Future {Testrun}, IterOnWorker, StartTime, EndTime)

	def toJSON(fullTestrun: Boolean = false): JsObject = Json.obj(
		"iterOnWorker" -> JsNumber(IterOnWorker)
	,	"start" -> JsNumber(StartTime)
	,	"end" -> JsNumber(EndTime)
	,	"testrun" -> (if(fullTestrun) getTestrun.toJSON else JsString(getTestrun.getID.toString))
	)

	def canEqual(other: Any): Boolean = other.isInstanceOf[LoadWorkerRaw]

	override def equals(other: Any): Boolean = other match {
		case that: LoadWorkerRaw =>
			getTestrun.equals(that.getTestrun) &&
			IterOnWorker == that.getIterOnWorker &&
			StartTime == that.getStart &&
			EndTime == that.getEnd
		case _ => false
	}

	override def hashCode(): Int = Seq(getTestrun, getIterOnWorker, getStart, getEnd).map(_.hashCode()).fold(0)((a,b) => a + b)
}