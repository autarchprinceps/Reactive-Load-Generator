import akka.actor.{Props, ActorRef, Inbox, ActorSystem}
import aktors.messages.{DBDelCMD, User}
import aktors.{DB, UIInstance}
import org.bson.types.ObjectId
import play.api.libs.json.{JsString, JsObject}

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

	def ws(what: Seq[(String, JsString)]) = inbox.send(uii, JsObject(what))

	def apply() = {
		testNotAuth
		testRegLogin
		testStorePlan
		testAllPlans
		testLoadRun
		drop
	}

	def testNotAuth = {

	}

	def testRegLogin = {
		val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
		for(i <- 0 to 200) {
			var name = "" + alphabet.charAt(i % alphabet.length)
			var password = "" + alphabet.charAt(i % alphabet.length)
			for(j <- 0 to i / 10 + 5) {
				name += alphabet.charAt((i + j + random.nextInt(i)) % alphabet.length)
				password += alphabet.charAt((i + j + random.nextInt(i)) % alphabet.length)
			}
			ws(List(
				("type", JsString("register"))
			,	("name", JsString(name))
			,	("password", JsString(password))
			))
			// TODO get answer
			// TODO login
			// TODO logout
		}

		ws(List(
			("type", JsString("register"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		ws(List(
			("type", JsString("login"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
	}

	def testStorePlan = {

	}

	def testAllPlans = {

	}

	def testLoadRun = {

	}

	def drop = {
		ws(List(
			("type", JsString("logout"))
		,	("name", JsString("test"))
		,	("password", JsString("test"))
		))
		// TODO get answer
		val cmd = new DBDelCMD
		cmd.t = DBDelCMD.Type.DB
		inbox.send(db, cmd)
	}
}
