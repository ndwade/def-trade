/*
 * Copyright 2014-2016 Panavista Technologies LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deftrade.db

import scala.language.postfixOps
import scala.xml.{ Elem, MetaData, Node, NodeSeq, Text, Utility }

import upickle.{ Js, json }

object XmlToJson extends ((NodeSeq, NodeSeq) => String) {

  /**
   * Conversts a forest of XML nodes to a JSON array.
   *
   * @param nodes the XML `Nodes` to convert.
   * @param excludes Limits the depth of the object nesting in the generated forest of JSON objects.
   * If during recusive conversion an XML [[Node]] in the `excludes` set is encountered, conversion
   * stops.
   * @return a `String` representing a JSON array.
   */
  def apply(nodes: NodeSeq, excludes: NodeSeq = NodeSeq.Empty): String = {

    def attrsToTaggedJsVals(attrs: MetaData): List[(String, Js.Str)] = attrs.asAttrMap.toList map {
      case (tag, value) => tag -> Js.Str(value)
    }

    def nodesToTaggedJsVals(nodes: Seq[Node]): Seq[(String, Js.Value)] = {

      val taggedJsVals = for {
        node <- nodes if !(excludes contains node)
        trimmedNode <- Utility trimProper node // FIXME: what's trimmedNode doing here?!
      } yield node.label -> nodeToJs(node)

      taggedJsVals groupBy { case (tag, _) => tag } mapValues {
        case Seq((_, jsVal)) => jsVal // no enclosing array for singleton
        case taggedJsVals => Js.Arr(taggedJsVals map (_._2): _*)
      } toList
    }

    def nodeToJs(node: Node): Js.Value = node match {
      case Text(s) => Js.Str(s)
      case Elem(_, _, xml.Null, _, ns @ _*) if ns.size == 1 && ns(0).isInstanceOf[Text] =>
        Js.Str(ns(0).text) // single text child - ugly but avoids compiler warning
      case Elem(_, _, attrs, _, children @ _*) =>
        Js.Obj(attrsToTaggedJsVals(attrs) ++ nodesToTaggedJsVals(children): _*)
      case unhandled => Js.Obj("#UNHANDLED" -> Js.Str(unhandled.text))
    }

    json write Js.Obj(nodesToTaggedJsVals(nodes): _*)
  }
}
