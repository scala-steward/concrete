package concrete;

import java.util.Properties
import scala.collection.JavaConversions
import scala.collection.mutable.HashMap
import scala.reflect.runtime.universe._
import scala.xml.NodeSeq
import scala.collection.mutable.HashSet
import scala.collection.immutable.TreeMap
import scala.collection.immutable.SortedMap

/**
 * This class is intended to hold Concrete's various parameters.
 *
 * @author vion
 *
 */
final class ParameterManager {

  private var _parameters: SortedMap[String, Any] = new TreeMap()

  val used = new HashSet[String]()

  /**
   * Updates some parameter, overriding default or previous value.
   *
   * @param name
   * @param value
   */
  def update(name: String, value: Any) {
    _parameters += (name -> value)
  }

  def getOrElse[T: TypeTag](name: String, default: => T): T = {
    get[T](name).getOrElse(default)
  }

  def get[T: TypeTag](name: String): Option[T] = {
    require(TypeTag.Nothing != typeTag[T], s"Please give a type for $name, was ${typeTag[T]}")
    val got = parameters.get(name).map {
      case s: String => parse(typeOf[T], s)
      case v: Any    => v.asInstanceOf[T]
    }
    if (got.isDefined) used += name
    got
  }

  def classInPackage[T](name: String, pack: String, default: => Class[T]): Class[T] = {
    (parameters.get(name) match {
      case Some(s: String) =>
        try Class.forName(s)
        catch {
          case _: ClassNotFoundException => Class.forName(s"$pack.$s")
        }
      case Some(c) => c
      case None    => default
    })
      .asInstanceOf[Class[T]]

  }

  def contains(name: String) = {
    val c = parameters.contains(name)
    if (c) used += name
    c
  }

  def unused = parameters.keySet -- used

  private def parse[T](fType: Type, value: String): T = {
    (if (fType <:< typeOf[Int]) {
      value.toInt
    } else if (fType <:< typeOf[Boolean]) {
      value.toBoolean
    } else if (fType <:< typeOf[Double]) {
      value.toDouble
    } else if (fType <:< typeOf[String]) {
      value
    } else if (fType <:< typeOf[Class[_]]) {
      Class.forName(value)
    } else {
      throw new IllegalArgumentException(s"Cannot parse $value of type $fType")
    }).asInstanceOf[T]

  }

  /**
   * Returns String representation of the registered parameters.
   *
   * @return
   */
  override def toString = _parameters.iterator.map {
    case (k, Unit) => k
    case (k, v)    => s"$k = $v"
  }.mkString(", ")

  def parameters: SortedMap[String, Any] = _parameters

  def parseProperties(line: Properties) {
    JavaConversions.mapAsScalaMap(line).foreach {
      case (k, v) => update(k.toString, v.toString)
    }
  }

}
