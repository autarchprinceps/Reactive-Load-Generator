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

class LoadWorkerRaw(Testrun: Testrun, IterOnWorker : Int, StartTime : Long, EndTime : Long) {
	def getTestrun: Testrun = Testrun // TODO async
	def getIterOnWorker: Int = IterOnWorker
	def getStart: Long = StartTime
	def getEnd: Long = EndTime

	def toJSON(fullTestrun: Boolean = false): JsObject = Json.obj(
		"iterOnWorker" -> JsNumber(IterOnWorker)
	,	"start" -> JsNumber(StartTime)
	,	"end" -> JsNumber(EndTime)
	,	"testrun" -> (if(fullTestrun) getTestrun.toJSON else JsString(Testrun.getID.toString))
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

	override def hashCode(): Int = {
		val state = Seq(Testrun, IterOnWorker, StartTime, EndTime)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}