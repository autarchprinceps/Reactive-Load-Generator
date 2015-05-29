package aktors.messages

import play.api.libs.json._
import scala.Tuple2
import scala.collection.JavaConversions
import java.net.MalformedURLException
import java.util.ArrayList

/**
 * Created by Patrick Robinson on 20.04.15.
 */
object LoadWorkerRaw {
	@throws(classOf[MalformedURLException])
	def fromJSON(raw: JsObject): LoadWorkerRaw = new LoadWorkerRaw(
		if(raw.\("testrun").isInstanceOf[JsObject]) Testrun.fromJSON(raw.\("testrun").asInstanceOf[JsObject]) else null
	,	raw.\("iterOnWorker").asInstanceOf[JsNumber].value.intValue
	,   raw.\("start").asInstanceOf[JsNumber].value.intValue
	,   raw.\("end").asInstanceOf[JsNumber].value.intValue
	)
}

class LoadWorkerRaw(testrun: Testrun, iterOnWorker : Int, startTime : Long, endTime : Long) {
	def getTestrun: Testrun = testrun // TODO async
	def getIterOnWorker: Int = iterOnWorker
	def getStart: Long = startTime
	def getEnd: Long = endTime

	def toJSON(fullTestrun: Boolean = false): JsObject = Json.obj(
		"iterOnWorker" -> JsNumber(iterOnWorker)
	,	"start" -> JsNumber(startTime)
	,	"end" -> JsNumber(endTime)
	,	"testrun" -> (if(fullTestrun) getTestrun.toJSON else JsString(testrun.id.toString))
	)

	def canEqual(other: Any): Boolean = other.isInstanceOf[LoadWorkerRaw]

	override def equals(other: Any): Boolean = other match {
		case that: LoadWorkerRaw =>
			getTestrun.equals(that.getTestrun) &&
			iterOnWorker == that.getIterOnWorker &&
			startTime == that.getStart &&
			endTime == that.getEnd
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(testrun, iterOnWorker, startTime, endTime)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}