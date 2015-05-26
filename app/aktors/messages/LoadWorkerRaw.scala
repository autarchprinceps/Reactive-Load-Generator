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
	private[this] var _testrun: Testrun = run // TODO async?

	def testrun: Testrun = _testrun

	def testrun_(value: Testrun): Unit = {
	  _testrun = value
	}

	private[this] var _iterOnWorker: Int = iter

	def iterOnWorker: Int = _iterOnWorker

	def iterOnWorker_(value: Int): Unit = {
	  _iterOnWorker = value
	}

	private[this] var _start: Long = startTime

	def start: Long = _start

	def start_(value: Long): Unit = {
	  _start = value
	}

	private[this] var _end: Long = endTime

	def end: Long = _end

	def end_(value: Long): Unit = {
	  _end = value
	}

	// def this() = this(null, 0, 0, 0)

	def toJSON(fullTestrun: Boolean = false): JsObject = Json.obj(
		"iterOnWorker" -> JsNumber(_iterOnWorker)
	,	"start" -> JsNumber(_start)
	,	"end" -> JsNumber(_end)
	,	"testrun" -> (if(fullTestrun) testrun.toJSON else JsString(_testrun.id.toString))
	)

	def canEqual(other: Any): Boolean = other.isInstanceOf[LoadWorkerRaw]

	override def equals(other: Any): Boolean = other match {
		case that: LoadWorkerRaw =>
			_testrun.equals(that.testrun) &&
			_iterOnWorker == that.iterOnWorker &&
			_start == that.start &&
			_end == that.end
		case _ => false
	}

	override def hashCode(): Int = {
		val state = Seq(_testrun, _iterOnWorker, _start, _end)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}