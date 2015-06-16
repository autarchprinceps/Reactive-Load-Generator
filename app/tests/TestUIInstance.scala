package tests

import java.net.URL
import java.util
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Inbox, Props}
import aktors.messages._
import aktors.{DB, UIInstance}
import com.typesafe.config.ConfigFactory
import helper.JSONHelper
import org.bson.types.ObjectId
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration
import scala.util.Random


/**
 * Created by Patrick Robinson on 12.05.15.
 */
class TestUIInstance {
	val as: ActorSystem = ActorSystem.create(
		"Test"
		,   ConfigFactory.load(
			ConfigFactory.parseString("akka.actor.dsl.inbox-size=1000000")
				.withFallback(ConfigFactory.load)
		)
	)
	val inbox : Inbox = Inbox.create(as)
	val uii : ActorRef = as.actorOf(UIInstance.props(inbox.getRef, true))
	val db : ActorRef = as.actorOf(Props(classOf[DB], "junit_loadgen"))
	val random : Random = new Random
	val problems : util.LinkedList[String] = new util.LinkedList[String]()

	def ws(what: Seq[(String, JsValue)]) = inbox.send(uii, JsObject(what).toString)

	def answerCheckType(typeToCheck : String) : Boolean = {
		val ans = get
		println("DEBUG: TestUIInstance answerCheckType " + typeToCheck + " : " + ans.toString)
		return ans.isInstanceOf[JsObject] && JSONHelper.JsStringToString(ans.asInstanceOf[JsObject].\("type")).equals(typeToCheck)
	}

	def get : JsObject = Json.parse(inbox.receive(Duration.create(10, TimeUnit.MINUTES)).asInstanceOf[String]).asInstanceOf[JsObject]

	def apply() : util.List[String] = {
		println("TestUIInstance start")
		testRegLogin
		println("TestUIInstance testRegLogin")
		Thread.sleep(2000)
		testStorePlan
		println("TestUIInstance testStorePlan")
		Thread.sleep(2000)
		testAllPlans
		println("TestUIInstance testAllPlans")
		Thread.sleep(2000)
		testRun
		println("TestUIInstance testRun")
		Thread.sleep(2000)
		testLoadPlan
		println("TestUIInstance testLoadPlan")
		Thread.sleep(2000)
		testLoadRun
		println("TestUIInstance testLoadRun")
		Thread.sleep(2000)
		drop
		println("TestUIInstance drop")
		Thread.sleep(2000)
		return problems
	}

	def testRegLogin = {
		val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
		val names = new ArrayBuffer[String]()
		val passwords = new ArrayBuffer[String]()
		for(i <- 0 until 20) {
			var name = "" + alphabet.charAt(i % alphabet.length)
			var password = "" + alphabet.charAt(i % alphabet.length)
			for (j <- 0 until i / 10 + 5) {
				name += alphabet.charAt((i + j + random.nextInt(i + 1)) % alphabet.length)
				password += alphabet.charAt((i + j + random.nextInt(i + 1)) % alphabet.length)
			}
			ws(List(
				("type", JsString("register"))
				, ("name", JsString(name))
				, ("password", JsString(password))
			))
			if (!answerCheckType("register")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Register failed: " + i + " " + name + " " + password))
			names += name
			passwords += password
		}
		Thread.sleep(2000)
		for(i <- 0 until 20) {
			ws(List(
				("type", JsString("login"))
			,	("name", JsString(names(i)))
			,	("password", JsString(passwords(i)))
			))
			if(!answerCheckType("login")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Login failed: " + i + " " + names(i) + " " + passwords(i)))
			ws(List(
				("type", JsString("logout"))
			,	("name", JsString(names(i)))
			,	("password", JsString(passwords(i)))
			))
			if(!answerCheckType("logout")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Logout failed: " + i + " " + names(i) + " " + passwords(i)))
			// TODO checkNotAuthenticated
			// TODO wrong authentication
		}

		ws(List(
			("type", JsString("register"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		if(!answerCheckType("registered")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Register failed: test test"))
		ws(List(
			("type", JsString("login"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		if(!answerCheckType("login")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Login failed: test test"))
	}

	val testplans = new mutable.HashMap[ObjectId, Testplan]()

	def testStorePlan = {
		for(k <- 0 until 50) {
			val tid = new ObjectId
			val ttp = new Testplan(
				ID = tid
			,   ConType = ConnectionType.HTTP
			,   NumRuns = k + random.nextInt(k + 1)
			,   Parallelity = random.nextInt(10) + 1
			,   Path = new URL("http://localhost:1301")
			)
			ws(List(
				("type", JsString("store plan"))
			,   ("testplan", ttp.toJSON(false))
			))
			testplans += tid -> ttp
			if(!answerCheckType("stored plan")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Store plan failed: " + ttp.toJSON(false).toString()))
		}
	}

	def testAllPlans = {
		ws(List(("type", JsString("all plans"))))
		val answer = get
		if(JSONHelper.JsStringToString(answer.\("type")).equals("all plans"))
		for(elem <- answer.\("content").asInstanceOf[JsArray].productIterator) {
			if(testplans.get(new ObjectId(JSONHelper.JsStringToString(elem.asInstanceOf[JsObject].\("id")))).isEmpty)
				problems.add(Test.problem(new Exception().getStackTrace()(0), "All plans failed"))
		}
	}

	val testruns = new mutable.HashMap[Testplan, ArrayBuffer[Testrun]]()

	def testRun = {
		for((_, tmptp) <- testplans) {
			val numTR = 3
			val tmpabuf = new ArrayBuffer[Testrun](numTR)
			for (j <- 0 until numTR) {
				ws(List(
					("type", JsString("start run"))
					, ("testplan", tmptp.toJSON(false))
				))
				val tmpget = get
				if (!tmpget.\("type").toString().equals("runstart")) {
					problems.add(Test.problem(new Exception().getStackTrace()(0), "run not started: " + tmpget))
				} else {
					val tmprun = Testrun.fromJSON(tmpget.\("content").asInstanceOf[JsObject])
					if (!tmprun.getTestplan.equals(tmptp))
						problems.add(Test.problem(new Exception().getStackTrace()(0), "Wrong Testplan for run: " + tmpget))
					tmpabuf += tmprun
					for (k <- 0 until tmptp.getNumRuns * tmptp.getParallelity) {
						if (!get.\("type").toString().equals("raw"))
							problems.add(Test.problem(new Exception().getStackTrace()(0), "Not enought raws received for Testrun: " + tmprun.toJSON(true)))
					}
				}
			}
			testruns += tmptp -> tmpabuf
		}
	}

	def testLoadPlan = {
		for((tmpid, tmptp) <- testplans) {
			ws(List(
				("type", JsString("load plan"))
			,	("id", JsString(tmptp.getID.toString))
			))
			val answer = get
			if(!JSONHelper.JsStringToString(answer.\("type")).equals("all runs"))
				problems.add(Test.problem(new Exception().getStackTrace()(0), "Loading plan failed: wrong type: " + answer))
			if(!tmpid.equals(new ObjectId(JSONHelper.JsStringToString(answer.\("testplan")))))
				problems.add(Test.problem(new Exception().getStackTrace()(0), "Loading plan failed: wrong plan: " + answer))
			for(elem <- answer.\("content").asInstanceOf[JsArray].productIterator) {
				if(!(testruns(tmptp) contains Testrun.fromJSON(elem.asInstanceOf[JsObject])))
					problems.add(Test.problem(new Exception().getStackTrace()(0), "Loading plan failed: wrong run: " + elem))
			}
		}
	}

	def testLoadRun = {
		for((_, tmptp) <- testplans) {
			val tmptrs = testruns(tmptp)
			for(j <- 0 until (tmptrs length)) {
				ws(List(
					("type", JsString("load run"))
				,	("id", JsString(tmptrs(j).getID.toString))
				))
				val recrun = get
				if(!recrun.\("type").toString().equals("testrun")) {
					problems.add(Test.problem(new Exception().getStackTrace()(0), "Load run failed: " + recrun))
				} else {
					Testrun.fromJSON(recrun.\("content").asInstanceOf[JsObject]).equals(tmptrs(j))
					for (k <- 0 until tmptp.getParallelity * tmptp.getNumRuns) {
						if(!get.\("type").toString().equals("raw"))
							problems.add(Test.problem(new Exception().getStackTrace()(0), "Too few raws received: " + recrun))
					}
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
		if(!answerCheckType("logout")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Final logout failed"))
		val cmd = new DBDelCMD
		cmd.t = DBDelCMD.Type.DB
		inbox.send(db, cmd)
	}
}
