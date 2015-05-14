import java.net.URL
import java.util.concurrent.TimeUnit

import akka.actor.{Props, Inbox, ActorSystem}
import aktors.messages.Testplan.ConnectionType
import aktors.messages.{Testrun, Testplan, DBDelCMD}
import aktors.{DB, UIInstance}
import org.fest.assertions.Assertions._
import play.api.libs.json.{JsValue, JsString, JsObject}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.util.Random

/**
 * Created by autarch on 12.05.15.
 */
object testUIInstance {
	val as = ActorSystem.create
	val inbox = Inbox.create(as)
	val uii = as.actorOf(Props.create(classOf[UIInstance], inbox.getRef, true))
	val db = as.actorOf(Props(classOf[DB]), "junit_loadgen")
	val random = new Random

	def ws(what: Seq[(String, JsValue)]) = inbox.send(uii, JsObject(what))

	def answerCheckType(typeToCheck : String) : Boolean = {
		val ans = get
		return ans.isInstanceOf[JsObject] && ans.asInstanceOf[JsObject].\("type").toString().equals(typeToCheck)
	}

	def get : JsObject = inbox.receive(Duration.create(1, TimeUnit.MINUTES)).asInstanceOf[JsObject]

	def apply() = {
		testNotAuth
		testRegLogin
		testStorePlan
		testAllPlans
		testRun
		testLoadPlan
		testLoadRun
		drop
	}

	def testNotAuth = {

	}

	def testRegLogin = {
		val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
		for(i <- 0 until 200) {
			var name = "" + alphabet.charAt(i % alphabet.length)
			var password = "" + alphabet.charAt(i % alphabet.length)
			for(j <- 0 until i / 10 + 5) {
				name += alphabet.charAt((i + j + random.nextInt(i)) % alphabet.length)
				password += alphabet.charAt((i + j + random.nextInt(i)) % alphabet.length)
			}
			ws(List(
				("type", JsString("register"))
			,	("name", JsString(name))
			,	("password", JsString(password))
			))
			assertThat(answerCheckType("registered"))
			ws(List(
				("type", JsString("login"))
			,	("name", JsString(name))
			,	("password", JsString(password))
			))
			assertThat(answerCheckType("login"))
			// TODO check authenticated
			ws(List(
				("type", JsString("logout"))
			,	("name", JsString(name))
			,	("password", JsString(password))
			))
			assertThat(answerCheckType("logout"))
			// TODO check not auth
		}

		ws(List(
			("type", JsString("register"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		assertThat(answerCheckType("registered"))
		ws(List(
			("type", JsString("login"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		assertThat(answerCheckType("login"))
	}

	val testplans = new ArrayBuffer[Testplan](10000)

	def testStorePlan = {
		for(i <- 0 until 10000) {
			val tmp = new Testplan()
			tmp.connectionType = ConnectionType.HTTP
			tmp.numRuns = i + random.nextInt(100 * i)
			tmp.parallelity = 1 + random.nextInt(20)
			tmp.path = new URL("http://localhost:1301") // TODO Server needs to be started at that address, from Java?
			tmp.waitBeforeStart = 0
			tmp.waitBetweenMsgs = 0
			ws(List(
				("type", JsString("store plan"))
			,	("testplan", tmp.toJSON(false))
			))
			testplans += tmp
		}
	}

	def testAllPlans = {
		ws(List(("type", JsString("all plans"))))
		for(i <- 0 until 10000) {
			val obj = get
			assertThat(obj.\("type").toString()).isEqualTo("testplan")
			assertThat(testplans contains(Testplan.fromJSON(obj.\("content").asInstanceOf[JsObject])))
		}
	}

	val testruns = new mutable.HashMap[Testplan, ArrayBuffer[Testrun]]()

	def testRun = {
		for(i <- 0 until (testplans length)) {
			val numTR = 1 + random.nextInt(10)
			val tmpabuf = new ArrayBuffer[Testrun](numTR)
			val tmptp = testplans(i)
			testruns put(tmptp, tmpabuf)
			for(j <- 0 until numTR) {
				ws(List(
					("type", JsString("start run"))
				,	("testplan", tmptp.toJSON(false))
				))
				val tmpget = get
				assertThat(tmpget.\("type").toString()).isEqualTo("runstart")
				val tmprun = Testrun.fromJSON(tmpget.\("content").asInstanceOf[JsObject])
				assertThat(tmprun.testplan.equals(tmptp))
				tmpabuf += tmprun
				for(k <- 0 until tmptp.numRuns * tmptp.parallelity) {
					assertThat(get.\("type").toString()).isEqualTo("raw")
				}
			}
		}
	}

	def testLoadPlan = {
		for(i <- 0 until (testplans length)) {
			val tmptp = testplans(i)
			ws(List(
				("type", JsString("load plan"))
			,	("id", JsString(tmptp.id.toString))
			))
			for(j <- 0 until (testruns(tmptp) length) + 1) {
				var response = get
				assertThat(
				(	get.\("type").toString().equals("testplan")
				&&	Testplan.fromJSON(get.\("content").asInstanceOf[JsObject]).equals(tmptp))
				||	(get.\("type").toString().equals("testrun")
				&&	(testruns(tmptp) contains Testrun.fromJSON(get.\("content").asInstanceOf[JsObject])))
				)
			}
		}
	}

	def testLoadRun = {
		for(i <- 0 until (testplans length)) {
			val tmptp = testplans(i)
			val tmptrs = testruns(tmptp)
			for(j <- 0 until (tmptrs length)) {
				ws(List(
					("type", JsString("load run"))
				,	("id", JsString(tmptrs(j).id.toString))
				))
				val recrun = get
				assertThat(recrun.\("type").toString()).isEqualTo("testrun")
				Testrun.fromJSON(recrun.\("content").asInstanceOf[JsObject]).equals(tmptrs(j))
				for(k <- 0 until tmptp.parallelity * tmptp.numRuns) {
					assertThat(get.\("type").toString()).isEqualTo("raw")
				}
			}
		}
	}

	def drop = {
		ws(List(
			("type", JsString("logout"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		assertThat(answerCheckType("logout"))
		val cmd = new DBDelCMD
		cmd.t = DBDelCMD.Type.DB
		inbox.send(db, cmd)
	}
}
