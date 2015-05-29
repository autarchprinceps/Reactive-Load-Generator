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

class User(ID: ObjectId = new ObjectId(), Name: String, Password: String = "") {
	override def hashCode: Int = ID.hashCode

	override def equals(other: Any): Boolean = other match {
		case user : User => user.getId.equals(getId)
		case json : JsObject => new ObjectId(JSONHelper.JsStringToString((json.\("id")))).equals(getId)
		case _ => false
	}

	def getName: String = Name
	def getId: ObjectId = ID

	private var _password: String = null
	def check(passwordCandidate: String): Boolean = _password == passwordCandidate
	def getPassword: String = _password
	def changePassword(oldPw: String, newPw: String): Boolean = {
		if (oldPw == Password) {
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
			"id" -> JsString(ID.toString)
		,   "name" -> JsString(Name)
		,   "password" -> JsString(Password)
		)
	else
		Json.obj(
			"id" -> JsString(ID.toString)
		,   "name" -> JsString(Name)
		)
}