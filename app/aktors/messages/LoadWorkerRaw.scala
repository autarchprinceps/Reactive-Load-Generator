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

class LoadWorkerRaw(run: Testrun, iter : Int, startTime : Long, endTime : Long) {
	private[this] var testrun: Testrun = run // TODO async?

	def getTestrun: Testrun = testrun

	def setTestrun(value: Testrun): Unit = {
	  testrun = value
	}

	private[this] var iterOnWorker: Int = iter

	def getIterOnWorker: Int = iterOnWorker

	def setIterOnWorker(value: Int): Unit = {
	  iterOnWorker = value
	}

	private[this] var start: Long = startTime

	def getStart: Long = start

	def setStart(value: Long): Unit = {
	  start = value
	}

	private[this] var end: Long = endTime

	def getEnd: Long = end

	def setEnd(value: Long): Unit = {
	  end = value
	}

	// def this() = this(null, 0, 0, 0)

	def toJSON(fullTestrun: Boolean = false): JsObject = Json.obj(
		"iterOnWorker" -> JsNumber(iterOnWorker)
	,	"start" -> JsNumber(start)
	,	"end" -> JsNumber(end)
	,	"testrun" -> (if(fullTestrun) getTestrun.toJSON else JsString(testrun.id.toString))
	)

	def canEqual(other: Any): Boolean = other.isInstanceOf[LoadWorkerRaw]

	override def equals(other: Any): Boolean = other match {
		case that: LoadWorkerRaw =>
			testrun.equals(that.getTestrun) &&
			iterOnWorker == that.getIterOnWorker &&
			start == that.getStart &&
			end == that.getEnd
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(testrun, iterOnWorker, start, end)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}