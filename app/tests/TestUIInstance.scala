package tests

import java.net.URL
import java.util
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Inbox, Props}
import aktors.messages.ConnectionType
import aktors.messages.{DBDelCMD, Testplan, Testrun}
import aktors.{DB, UIInstance}
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{Json, JsObject, JsString, JsValue}

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
	val inbox = Inbox.create(as)
	val uii = as.actorOf(Props.create(classOf[UIInstance], inbox.getRef, true.asInstanceOf[AnyRef]))
	val db = as.actorOf(Props(classOf[DB]), "junit_loadgen")
	val random = new Random
	val problems = new util.LinkedList[String]()

	def ws(what: Seq[(String, JsValue)]) = inbox.send(uii, JsObject(what).toString)

	def answerCheckType(typeToCheck : String) : Boolean = {
		val ans = get
		return ans.isInstanceOf[JsObject] && ans.asInstanceOf[JsObject].\("type").toString().equals(typeToCheck)
	}

	def get : JsObject = Json.parse(inbox.receive(Duration.create(10, TimeUnit.MINUTES)).asInstanceOf[String]).asInstanceOf[JsObject]

	def apply() : util.List[String] = {
		println("TestUIInstance start")
		testNotAuth
		println("TestUIInstance testNotAuth")
		testRegLogin
		println("TestUIInstance testRegLogin")
		testStorePlan
		println("TestUIInstance testStorePlan")
		testAllPlans
		println("TestUIInstance testAllPlans")
		testRun
		println("TestUIInstance testRun")
		testLoadPlan
		println("TestUIInstance testLoadPlan")
		testLoadRun
		println("TestUIInstance testLoadRun")
		drop
		println("TestUIInstance drop")
		return problems
	}

	def testNotAuth = {
		// TODO
	}

	def testRegLogin = {
		val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
		for(i <- 0 until 20) {
			var name = "" + alphabet.charAt(i % alphabet.length)
			var password = "" + alphabet.charAt(i % alphabet.length)
			for(j <- 0 until i / 10 + 5) {
				name += alphabet.charAt((i + j + random.nextInt(i+1)) % alphabet.length)
				password += alphabet.charAt((i + j + random.nextInt(i+1)) % alphabet.length)
			}
			ws(List(
				("type", JsString("register"))
			,	("name", JsString(name))
			,	("password", JsString(password))
			))
			if(!answerCheckType("registered")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Register failed: " + i + " " + name + " " + password))
			ws(List(
				("type", JsString("login"))
			,	("name", JsString(name))
			,	("password", JsString(password))
			))
			if(!answerCheckType("login")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Login failed: " + i + " " + name + " " + password))
			// TODO check authenticated
			ws(List(
				("type", JsString("logout"))
			,	("name", JsString(name))
			,	("password", JsString(password))
			))
			if(!answerCheckType("logout")) problems.add(Test.problem(new Exception().getStackTrace()(0), "Logout failed: " + i + " " + name + " " + password))
			// TODO check not auth
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

	val testplans = new ArrayBuffer[Testplan](50)

	def testStorePlan = {
		for(i <- 0 until 50) {
			val tmp = new Testplan()
			tmp.setConnectionType(ConnectionType.HTTP)
			tmp.setNumRuns(i + random.nextInt(i + 1))
			tmp.setParallelity(1 + random.nextInt(10))
			tmp.setPath(new URL("http://localhost:1301")) // TODO Server needs to be started at that address, from Java?
			tmp.setWaitBeforeStart(0)
			tmp.setWaitBetweenMsgs(0)
			ws(List(
				("type", JsString("store plan"))
			,	("testplan", tmp.toJSON(false))
			))
			testplans += tmp
		}
	}

	def testAllPlans = {
		ws(List(("type", JsString("all plans"))))
		for(i <- 0 until 50) {
			val obj = get
			if(!(obj.\("type").toString()).equals("testplan") && (testplans contains(Testplan.fromJSON(obj.\("content").asInstanceOf[JsObject]))))
				problems.add(Test.problem(new Exception().getStackTrace()(0), "All plans failed: " + i + " result: " + obj.toString()))
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
				if(!tmpget.\("type").toString().equals("runstart")) {
					problems.add(Test.problem(new Exception().getStackTrace()(0), "run not started: " + tmpget))
				} else {
					val tmprun = Testrun.fromJSON(tmpget.\("content").asInstanceOf[JsObject])
					if(!tmprun.testplan.equals(tmptp))
						problems.add(Test.problem(new Exception().getStackTrace()(0), "Wrong Testplan for run: " + tmpget))
					tmpabuf += tmprun
					for (k <- 0 until tmptp.getNumRuns * tmptp.getParallelity) {
						if(!get.\("type").toString().equals("raw"))
							problems.add(Test.problem(new Exception().getStackTrace()(0), "Not enought raws received for Testrun: " + tmprun.toJSON(true)))
					}
				}
			}
		}
	}

	def testLoadPlan = {
		for(i <- 0 until (testplans length)) {
			val tmptp = testplans(i)
			ws(List(
				("type", JsString("load plan"))
			,	("id", JsString(tmptp.getId.toString))
			))
			for(j <- 0 until (testruns(tmptp) length) + 1) {
				var response = get
				if(!
				(	get.\("type").toString().equals("testplan")
				&&	Testplan.fromJSON(get.\("content").asInstanceOf[JsObject]).equals(tmptp))
				||	(get.\("type").toString().equals("testrun")
				&&	(testruns(tmptp) contains Testrun.fromJSON(get.\("content").asInstanceOf[JsObject])))
				)
					problems.add(Test.problem(new Exception().getStackTrace()(0), "Loading plan failed: " + response))
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
