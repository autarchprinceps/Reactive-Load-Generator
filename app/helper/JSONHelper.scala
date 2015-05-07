package helper

import play.api.libs.json.{JsString, JsObject}

/**
 * Created by Patrick Robinson on 07.05.2015.
 */
object JSONHelper {
	def simpleResponse(typ : String, description : String) : JsObject = new JsObject(List(
		("type", JsString(typ))
	,   ("description", JsString(description))
	))

	def objectResponse(typ : String, obj: JsObject) : JsObject = new JsObject(List(
		("type", JsString(typ))
	,   ("content", obj)
	))
}
