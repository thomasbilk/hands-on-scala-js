package webpage

import org.scalajs.dom
import org.scalajs.dom.{Node, Element}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scalatags.JsDom.all._

@JSExport
object Weather1 extends{
  @JSExport
  def main(target: dom.HTMLDivElement) = {
    import dom.extensions._
    import scala.scalajs
                .concurrent
                .JSExecutionContext
                .Implicits
                .runNow

    val url =
      "http://api.openweathermap.org/" +
      "data/2.5/weather?q=Singapore"

    Ajax.get(url).onSuccess{ case xhr =>
      target.appendChild(
        pre(xhr.responseText).render
      )
    }
  }
}