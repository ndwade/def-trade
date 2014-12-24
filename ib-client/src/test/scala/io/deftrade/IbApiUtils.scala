/*
 * Copyright 2014 Panavista Technologies, LLC
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
package io.deftrade

import reflect.runtime.{ universe => ru }
import org.scalatest.Assertions.fail

trait IbApiUtils {

  type ErrorMessage = String
  type HmVal = List[ErrorMessage]
  val good: HmVal = Nil
  def bad(msg: ErrorMessage) = List(msg)
  def acc(x: HmVal, y: HmVal): HmVal = x ::: y

  final def assert(hv: HmVal): Unit = hv match {
    case Nil => ()
    case msgs => fail(msgs.mkString("; "))
  }
  /*
   * Negate the semantics of the Assertions.Equalizer.=== method. 
   * Use when you'd otherwise use !=
   */
  def negate(hv: HmVal): HmVal = hv match {
    case Nil => bad("operands equal when non-equal was expected")
    case _ => good
  }
  /**
   * Given an instance of a TWB DTO object or field, check agains the expected com.ib.client
   * instance or field. Uses simple conventions for converting scalar or simple container values,
   * and recursively evaluates the TWB DTO case classes (field by field).
   */
  //  private[this] val m = ru.runtimeMirror(getClass.getClassLoader)
  private[this] lazy val m = reflect.runtime.currentMirror

  private[deftrade] def mirrorAndType(x: Any) = {
    val im = m.reflect(x)
    (im, im.symbol.asType.toType)
  }

  private implicit class Equalogizer(l: Any) { // sic
    def ===(r: Any)(implicit context: String): HmVal = 
      if (l == r) good 
      else bad(s"$context: $l != $r")
  }

  implicit class Homologizer(x: Any) {

    def =~=(y: Any)(implicit context: String = x.getClass.getSimpleName()): HmVal = {

      x match {

        case _: Int | _: Long | _: Double | _: Boolean => x === y

        case s: String =>
          val y2 = if (y == null) "" else y.asInstanceOf[String]
          s === y2

        case e: Enumeration#Value =>
          import ResiliantRx.PREFIX
          val err = e.toString.startsWith(PREFIX) // expected when testing Incoming
          val x = e.toString.stripPrefix(PREFIX)
          val y2 = if (y != null) y else ""
          y2 match {
            case i: Int =>
              if (err) x.toInt === i
              // lenience for sloppy ib code on order.volType and referencePriceType
              // Undefined (name == empty string) ordinal enum can map to Int.MaxValue or zero
              else if (e.toString == "" && i == Int.MaxValue) good
              else e.id === i;
            case s: String if RxString.isEmptyStringOrAlias(s) =>
              x === ""
            case s: String =>
              x === s
          }

        case g: GenId => g.id === y

        case Some(xx) => xx =~= y

        case None => y match {
          case i: Int => Int.MaxValue === y
          case d: Double => Double.MaxValue === y
          case null => good
        }
        case (tag, value) =>
          val tvy = y.asInstanceOf[com.ib.client.TagValue]
          acc(tag =~= tvy.m_tag, value =~= tvy.m_value)

        case list: List[_] => (list, y.asInstanceOf[java.util.Vector[_]]) match {
          case (Nil, null) => good
          case (_, null) => bad(s"nonempty list ${list} matched to null vector")
          case (_, vy) =>
            if (list.length != vy.size()) bad(s"lengths don't match for $list, $vy") else {
              list.zipWithIndex.foldLeft(good) { (b, a) =>
                a match { case (x, i) => acc(b, x =~= vy.get(i)) }
              }
            }
        }
        case cc => { // assume we're looking at a DTO case class
          val (imx, xTpe) = mirrorAndType(x)
          val (imy, yTpe) = mirrorAndType(y)
          val outerContext = context
          val ret = for {
            sym <- xTpe.decls if sym.isTerm && sym.asTerm.isVal
          } yield {
            implicit val context = s"$outerContext.${sym.name.decodedName.toString.trim}"
            val xx = imx.reflectField(sym.asTerm).get
            val ySym = yTpe.decl(ru.TermName(s"m_${sym.name}".trim))
            val yy = imy.reflectField(ySym.asTerm).get
            xx =~= yy
          }
          ret.foldLeft(good)(acc)
        }
      }
    }
  }

  /**
   * Given an instance of any domain object or field of a domain object in the TWB DTO API,
   * create the same instance for the com.ib.client API.
   * 	=> Case classes are mapped to their homologous ib classes
   * 	=> field values are converted according to simple and systematic conventions.
   */
  def ibCopyOf(x: Any, parent: AnyRef = null, sym: ru.Symbol = ru.NoSymbol): Any = {

    def assignIbField(field: Any, sym: ru.Symbol, toIm: ru.InstanceMirror, toType: ru.Type) {
      // println(s"assigning ${field} from ${sym}")
      val ibName = ru.TermName(s"m_${sym.name}".trim)
      val ibSym = toType.member(ibName)
      val ibType = ibSym.typeSignature
      val fm = toIm.reflectField(ibSym.asTerm)
      val value = field match {
        case e: Enumeration#Value => if (ibType =:= ru.definitions.IntTpe) e.id else e.toString
        case _ => field
      }
      // println(s"assigning ${value} to ${ibSym} as type ${ibType}")
      fm.set(value)
    }
    def ibHomologFor(cc: AnyRef with Product): (AnyRef, ru.InstanceMirror, ru.Type) = {
      val cls = m.staticClass(s"com.ib.client.${cc.productPrefix}")
      val cType = cls.asType.toType
      val ctors = cType.member(ru.termNames.CONSTRUCTOR).asTerm.alternatives
      val naCtor = ctors find { sym => // this idiom finds the (no-arg) constructor
        sym.asMethod.paramLists match {
          case List(List()) => true
          case _ => false
        }
      }
      val cm = m.reflectClass(cls).reflectConstructor(naCtor.get.asMethod)
      val ibc = cm.apply().asInstanceOf[AnyRef with Product] // Product, because we know this is DTO
      (ibc, m.reflect(ibc), cType)
    }

    // println(x)
    x match {
      case _: Int | _: Long | _: Double | _: Boolean | _: String => x
      case e: Enumeration#Value => e // (sic) - have to let assignIbField figure it out
      case g: GenId => g.id
      case sx @ Some(xx) => xx match {
        case i: Int => i
        case d: Double => d
        case ar: AnyRef with Product => ibCopyOf(ar, sx)
      }
      case None => { // enhanced interrogation techniques to determine type
        import ru.TypeRefTag
        import ru.definitions.{ IntTpe, DoubleTpe }
        val fm = m.reflect(parent).reflectField(sym.asTerm)
        val fType = fm.symbol.typeSignature
        val ru.TypeRef(_, _, args) = fType
        val tpe = args.head // we know we're dealing with an Option
        // println(s"type for ${sym} is Option[${tpe}]")
        if (tpe =:= IntTpe) Int.MaxValue
        else if (tpe =:= DoubleTpe || (tpe.toString endsWith "MoneyType"))
          Double.MaxValue // RUDE HACK only works for ib test where we know MoneyType is Double 
        else null
      }
      case (tag: String, value: String) => new com.ib.client.TagValue(tag, value)
      case list: List[_] => { // This handling for Vector will work for all cases!
        val ibVec = new java.util.Vector[Any](list.length)
        list foreach { el => ibVec add ibCopyOf(el) }
        ibVec
      }
      case cc: AnyRef with Product => {
        val (ccim, ccType) = mirrorAndType(cc)
        val (ibc, ibcIm, ibcType) = ibHomologFor(cc)
        for (sym <- ccType.decls if sym.isTerm && sym.asTerm.isVal) {
          val field = ccim.reflectField(sym.asTerm).get
          val ibField = ibCopyOf(field, cc, sym)
          assignIbField(ibField, sym, ibcIm, ibcType)
        }
        ibc
      }
    }
  }

  // reflecting on Enumerations:
  // http://stackoverflow.com/questions/12128783/how-can-i-get-the-actual-object-referred-to-by-scala-2-10-reflection?rq=1
  // http://stackoverflow.com/questions/12129172/using-scala-2-10-reflection-how-can-i-list-the-values-of-enumeration
  import util.{ Random => StdRand }
  //  import org.scalacheck.{ Gen, Arbitrary }
  val ints = Array(103, 107, 113, 123, 133, 142, 162, 199)
  val longs = ints map { i => (i * 10).asInstanceOf[Long] }
  val doubles = Array(1.41, 1.618, 2.72, 3.14, 19.47)
  val strings = Array("foo", "bar", "baz", "clem", "zorp", "twerk")
  def oneOf[T](ts: Array[T]): T = ts(StdRand.nextInt(ts.length))
  def generate[T](implicit tt: ru.TypeTag[T], outer: ru.InstanceMirror): T =
    generate(tt.tpe).asInstanceOf[T]
  def generate(tpe: ru.Type)(implicit outer: ru.InstanceMirror): Any = {
    // TODO: conditional expansion is ugly but =:= and <:< aren't friendly to pattern matching... ?!
    import ru._
    import definitions._
    if (tpe =:= IntTpe) oneOf(ints)
    else if (tpe =:= DoubleTpe || (tpe.toString endsWith "MoneyType")) oneOf(doubles) // HACK
    else if (tpe =:= LongTpe) oneOf(longs)
    else if (tpe =:= BooleanTpe) StdRand.nextBoolean
    else if (tpe =:= typeOf[String] ||
      (tpe.toString endsWith "ExchangeType") ||
      (tpe.toString endsWith "CurrencyType")) { oneOf(strings) } // HACK
    //    else if (tpe <:< typeOf[IbDomainTypesComponent#ExchangeType]) ??? // doesn't fire
    else if (tpe =:= typeOf[ConId]) new ConId(333)
    else if (tpe =:= typeOf[ReqId]) new ReqId(444)
    else if (tpe =:= typeOf[OrderId]) new OrderId(555)
    else if (tpe =:= typeOf[TickId]) new TickId(777)
    else if (tpe =:= typeOf[(String, String)]) (oneOf(strings), oneOf(strings))
    else if (tpe <:< typeOf[Option[_]]) {
      val TypeRef(_, _, List(argType)) = tpe
      Option(generate(argType))
    } else if (tpe <:< typeOf[List[_]]) {
      val TypeRef(_, _, List(argType)) = tpe
      //      println(s"List argType = $argType")
      val n = StdRand.nextInt(4) + 1
      List.fill(n)(generate(argType))
    } else if (tpe <:< typeOf[Enumeration#Value]) {
      val TypeRef(enumType, _, _) = tpe
      val enumModule = enumType.typeSymbol.asClass.module.asModule
      val e = m.reflectModule(enumModule).instance.asInstanceOf[Enumeration]
      oneOf(e.values.toArray)
    } else if (tpe <:< typeOf[DTOs#DTO]) {
      //      println(s"DTO type: $tpe")
      val cts = tpe.decl(termNames.CONSTRUCTOR).asMethod
      val cm = outer.reflectClass(tpe.typeSymbol.asClass)
      val ctorMirror = cm.reflectConstructor(cts)
      val params = cts.paramLists.head map { sym => { generate(sym.typeSignature) } }
      // println(s"finished gathering params for ${tpe}: $params")
      ctorMirror(params: _*)
    } else fail(s"DTO generation encountered unexpected type: ${tpe}")
  }

  //  implicit val arbitraryContract = Arbitrary {
  //    Gen { params => Some(generate(typeOf[Contract])) }
  //  }

}