package aktors.messages

import helper.JSONHelper
import org.bson.types.ObjectId
import play.api.libs.json._
import scala.Tuple2
import scala.collection.JavaConversions
import java.util.ArrayList

/**
 * Created by Patrick Robinson on 02.05.15.
 */
object User {
	def fromJSON(user: JsObject): User = new User(
		new ObjectId(user.\("id").toString),
		user.\("name").toString,
		if (user.\("password").isInstanceOf[JsString]) JSONHelper.JsStringToString(user.\("password")) else ""
	)
}

class User(id: ObjectId = new ObjectId(), name: String, password: String = "") {
	override def hashCode: Int = id.hashCode

	override def equals(other: Any): Boolean = other match {
		case user : User => user.id.equals(id)
		case json : JsObject => new ObjectId(JSONHelper.JsStringToString((json.\("id")))).equals(id)
		case _ => false
	}

	def getName: String = name
	def getId: ObjectId = id

	private var _password: String = null
	def check(passwordCandidate: String): Boolean = _password == passwordCandidate
	def getPassword: String = _password
	def changePassword(oldPw: String, newPw: String): Boolean = {
		if (oldPw == password) {
			_password = newPw
			return true
		}
		else {
			return false
		}
	}

	def toJSON: JsObject = toJSON(false)
	def toJSON(withPw: Boolean): JsObject = if(withPw)
		Json.obj(
			"id" -> JsString(id.toString)
		,   "name" -> JsString(name)
		,   "password" -> JsString(password)
		)
	else
		Json.obj(
			"id" -> JsString(id.toString)
		,   "name" -> JsString(name)
		)
}