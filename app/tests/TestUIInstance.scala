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
		Thread.sleep(5000)
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
		}

		ws(List(
			("type", JsString("register"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		if(!answerCheckType("register")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Register failed: test test"))
		ws(List(
			("type", JsString("login"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		if(!answerCheckType("login")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Login failed: test test"))
	}

	val testplans = new mutable.HashMap[ObjectId, Testplan]()

	def testStorePlan = {
		for(k <- 0 until 10) {
			val tid = new ObjectId
			val ttp = new Testplan(
				ID = tid
			,   ConType = ConnectionType.HTTP
			,   NumRuns = 3 + random.nextInt(k + 1)
			,   Parallelity = random.nextInt(4) + 1
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
		println("tUII testAllPlans got")
		if(JSONHelper.JsStringToString(answer.\("type")).equals("all plans")) {
			println("tUII testAllPlans checking individual plans orig size: " + testplans.size + " answer size: " + answer.\("content").asInstanceOf[JsArray].value.size)
			for(elem <- answer.\("content").asInstanceOf[JsArray].value) {
				if(testplans.get(new ObjectId(JSONHelper.JsStringToString(elem.asInstanceOf[JsObject].\("id")))).isEmpty) {
					println(elem)
					problems.add(Test.problem(new Exception().getStackTrace()(0), "A plan in all plans failed"))
				}
			}
		} else {
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
				,   ("testplan", JsString(tmptp.getID.toString))
				))
				var tmprun : Testrun = null
				for(k <- 0 until tmptp.getNumRuns * tmptp.getParallelity + 1) { // Testrun + Raws
					val tmpget = get
					val gettype = JSONHelper.JsStringToString(tmpget.\("type"))
					if(tmprun == null && gettype.equals("testrun")) {
						tmprun = Testrun.fromJSON(tmpget.\("content").asInstanceOf[JsObject])
					} else if(!gettype.equals("raw")) {
						problems.add(Test.problem(new Exception().getStackTrace()(0), "Unexpected message in testRun: " + tmpget))
					}
				}
				if(tmprun == null)
					problems.add(Test.problem(new Exception().getStackTrace()(0), "No testrun definition for run received"))
				else tmpabuf += tmprun
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
			for(elem <- answer.\("content").asInstanceOf[JsArray].value) {
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
				var tmprun : Testrun = null
				for(k <- 0 until tmptp.getNumRuns * tmptp.getParallelity + 1) { // Testrun + Raws
				val tmpget = get
					val gettype = JSONHelper.JsStringToString(tmpget.\("type"))
					if(tmprun == null && gettype.equals("testrun")) {
						tmprun = Testrun.fromJSON(tmpget.\("content").asInstanceOf[JsObject])
						if(!tmprun.equals(tmptrs(j)))
							problems.add(Test.problem(new Exception().getStackTrace()(0), "Wrong testrun definition for run received"))
					} else if(!gettype.equals("raw")) {
						problems.add(Test.problem(new Exception().getStackTrace()(0), "Unexpected message in loadRun: " + tmpget))
					}
				}
				if(tmprun == null)
					problems.add(Test.problem(new Exception().getStackTrace()(0), "No testrun definition for run received"))
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
