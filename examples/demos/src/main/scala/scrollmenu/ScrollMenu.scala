package scrollmenu

import org.scalajs.dom

import scala.scalajs.js
import scalatags.JsDom.all._

case class Tree[T](value: T, children: Vector[Tree[T]])

case class MenuNode(frag: dom.HTMLElement, id: String, start: Int, end: Int)

/**
 * High performance scrollspy to work keep the left menu bar in sync.
 * Lots of sketchy imperative code in order to maximize performance.
 */
class ScrollSpy(structure: Tree[String],
                main: dom.HTMLElement){
  val (headers, domTrees) = {
    var i = -1
    def recurse(t: Tree[String], depth: Int): Tree[MenuNode] = {
      val curr =
        li(
          a(
            t.value,
            display := (if (i == -1) "none" else "block"),
            href:="#"+Controller.munge(t.value),
            cls:="menu-item"
          )
        )
      val originalI = i
      i += 1
      val children = t.children.map(recurse(_, depth + 1))
      Tree(
        MenuNode(
          curr(ul(marginLeft := "15px",children.map(_.value.frag))).render,
          Controller.munge(t.value),
          originalI,
          if (children.length > 0) children.map(_.value.end).max else originalI + 1
        ),
        children
      )
    }
    def offset(el: dom.HTMLElement, parent: dom.HTMLElement): Double = {
      if (el == parent) 0
      else el.offsetTop + offset(el.offsetParent.asInstanceOf[dom.HTMLElement], parent)
    }
    val headers = {
      val menuItems = {
        def rec(current: Tree[String]): Seq[String] = {
          current.value +: current.children.flatMap(rec)
        }
        rec(structure).tail
      }

      js.Array(
        menuItems.map(Controller.munge)
          .map(dom.document.getElementById)
          .map(offset(_, main)):_*
      )
    }
    val domTrees = recurse(structure, 0)
    (headers, domTrees)
  }

  var open = false
  def toggleOpen() = {
    open = !open
    if (open){
      def rec(tree: Tree[MenuNode])(f: MenuNode => Unit): Unit = {
        f(tree.value)
        tree.children.foreach(rec(_)(f))
      }
      rec(domTrees)(setFullHeight)
    }else{
      start(force = true)
    }
  }

  def setFullHeight(mn: MenuNode) = {
    mn.frag
      .children(1)
      .asInstanceOf[dom.HTMLElement]
      .style
      .maxHeight = (mn.end - mn.start + 1) * 44 + "px"
  }
  private[this] var scrolling = false
  def apply() = {
    if (!scrolling) {
      println("Scroll...")
      scrolling = true
      dom.requestAnimationFrame((d: Double) => start())
    }
  }
  private[this] var previousWin: MenuNode = null
  private[this] def start(force: Boolean = false) = {
    scrolling = false
    def scroll(el: dom.Element) = {
      val rect = el.getBoundingClientRect()
      if (rect.top <= 0)
        el.scrollIntoView(true)
      else if (rect.top > dom.innerHeight)
        el.scrollIntoView(false)
    }
    val scrollTop = main.scrollTop
    def walkIndex(tree: Tree[MenuNode]): List[Tree[MenuNode]] = {
      val t @ Tree(m, children) = tree
      val win = if(m.start == -1) true
      else {
        val before = headers(m.start) <= scrollTop
        val after = (m.end >= headers.length) || headers(m.end) > scrollTop
        before && after
      }
      val childIndexes = children.map(walkIndex)
      val childWin = childIndexes.indexWhere(_ != null)
      if (childWin != -1) t :: childIndexes(childWin)
      else if (win) List(t)
      else null
    }

    val winPath = walkIndex(domTrees)
    val winItem = winPath.last.value
    def walkTree(tree: Tree[MenuNode], indices: List[Tree[MenuNode]]): Unit = {
      println("WalkTree")
      for(Tree(mn, children) <- indices){
        mn.frag.classList.remove("hide")
        mn.frag.classList.remove("selected")
        setFullHeight(mn)
        mn.frag.children(0).classList.add("pure-menu-selected")
        for(child <- children if child.value.frag != indices(1).value.frag){
          val childFrag = child.value.frag

          childFrag.children(0).classList.remove("pure-menu-selected")
          childFrag.classList.add("hide")
          if(!open)
            childFrag.children(1).asInstanceOf[dom.HTMLElement].style.maxHeight = "0px"

          if (child.value.start < winItem.start) childFrag.classList.add("selected")
          else childFrag.classList.remove("selected")
        }
      }

    }

    if (winItem != previousWin || force){
      scroll(winItem.frag.children(0))
      dom.history.replaceState(null, null, "#" + winItem.id)
      previousWin = winItem
      walkTree(domTrees, winPath)
    }

  }


}