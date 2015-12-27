/*
 * Copyright 2014-2016 Panavista Technologies, LLC
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

/**
 * @author ndw
 */

import com.github.tminglei.slickpg._

object Config {
  val dbconfig = "pgndw"
  val initScripts = Seq( /* "drop-tables.sql", "create-tables.sql", "populate-tables.sql" */ )
  val excluded = List.empty[String]
  val pkg = "io.deftrade.db"
}

/**
 *  This customizes the Slick code generator. We only do simple name mappings.
 *  For a more advanced example see https://github.com/cvogt/slick-presentation/tree/scala-exchange-2013
 */
object SourceCodeGenerator {
  import scala.concurrent.ExecutionContext.Implicits.global
  import Config._
  import DefTradePgDriver.{ createModel, defaultTables, api }
  import api._

  def main(args: Array[String]) = {

    for (script <- initScripts) {
      
      val cmd = s"psql -U test -d test -f ./src/sql/$script"
      val exec = Runtime.getRuntime().exec(cmd);
      
      exec.waitFor() match {
        case 0 => println(s"$script finished.")
        case errno => println(s"$script exited with error code $errno")
      }
    }

    val db = Database.forConfig(dbconfig)

    val modelAction = createModel(Option(
      defaultTables map { _ filterNot { mt => excluded contains mt.name } }))
    val modelFuture = db run modelAction

    val codegenFuture = modelFuture map { model => new SourceCodeGenerator(model) } onSuccess {
      case codegen => codegen.writeToFile(
        profile = "DefTradePgDriver",
        folder = "src/main/scala",
        pkg = pkg,
        container = "Tables",
        fileName = "Tables.scala")
    }
  }
}

class SourceCodeGenerator(model: slick.model.Model) extends slick.codegen.SourceCodeGenerator(model) {
  import slick.profile.SqlProfile.ColumnOption.SqlType
  import slick.ast.{ ColumnOption => co }
  import Config.pkg

  private val RxIes = "(.*)ies".r
  private val RxS = "(.*)s".r
  private val RxEn = "(.*x)en".r

  protected[db] def depluralize(s: String) = s match {
    case t if t.toLowerCase endsWith "series" => t + "Row" 
    case RxIes(t) => t + 'y' // parties => party
    case RxS(t) => t // cars => car
    case RxEn(t) => t // oxen => ox
    case t => t + "Row" // sheep => sheepRow (needed to disambiguate) 
  }

  override def entityName = (dbName: String) => depluralize(dbName.toCamelCase)

  // single columns of type int or long with name id MUST be primary keys by convention.
  // by convention, primary keys of sql type int or long, which have the name "id",
  // will get type safe, value class based wrapper types.
  private val tsIdTypes = Set("Int", "Long")

  import slick.{ model => m }

  private def isPk(col: m.Column) = (col.options contains co.PrimaryKey) && col.name == "id" && (tsIdTypes contains col.tpe)

  def tsIdPkCol(cols: Seq[m.Column]): Option[m.Column] = cols match {
    case Seq(col) if isPk(col) => Some(col)
    case _ => None
  }
  private def tsIdName(col: m.Column) = entityName(tableName(col.table.asString)) + "Id"

  private val tsPkDefsCode = List.newBuilder[Option[String]]

  override def Table = new Table(_) { tableDef =>

    // note tableDef.model.primaryKey build is disabled for single col pks... o.O
    val tsIdPkOptCol = tableDef.model.columns find isPk

    val tsPkCode = for {
      col <- tsIdPkOptCol
      name = tsIdName(col)
    } yield s"""
      
case class $name(val value: ${col.tpe}) extends AnyVal with slick.lifted.MappedTo[${col.tpe}]
object $name extends IdCompanion[$name]
  """
    tsPkDefsCode += tsPkCode

    val tsIdFkCol = (for {
      fk <- tableDef.model.foreignKeys
      col <- tsIdPkCol(fk.referencedColumns)
    } yield (fk.referencingColumns(0), col)).toMap

    override def autoIncLastAsOption = true

    override def Column = new Column(_) { columnDef =>

      def mapSqlType(typeName: String): String = {
        val RxEnum = "(.*)_e".r
        val RxArray = """_(.*)""".r
        val ret = typeName match {
          case RxArray(tn) => s"List[${mapSqlType(tn)}]"
          case RxEnum(en) => s"$pkg.${en.toCamelCase}.${en.toCamelCase}"
          case "money" => "BigDecimal"
          case "uuid" => "UUID"
          case "text" => "String"
          case "int8" => "Long"
          case "int4" => "Int"
          case "int2" => "Short"
          case "float4" => "Float"
          case "float8" => "Double"
          case "bool" => "Boolean"
          case "date" => "java.time.LocalDate"
          case "time" => "java.time.LocalTime"
          case "timestamp" => "java.time.LocalDateTime"
          case _ => s"${super.rawType}"
        }
        println(s"${columnDef.name}: $typeName => $ret")
        ret
      }
      /**
       * @returns the full scala type mapped from the SQL type given in the model.
       * Note that this method will basically make an array out of any type; if this is
       * not supported it should not compile (fail to find implicits) when the whole model
       * is assembled. So we take the easy path here; wire walk with a net ;) 
       */
      override def rawType: String = columnDef.model match {
        case col if tsIdPkOptCol contains col => s"$pkg.${tsIdName(col)}"
        case col if tsIdFkCol.keySet contains col => s"$pkg.${tsIdName(tsIdFkCol(col))}"
        case col => col.options collectFirst {
          case SqlType(typeName) => mapSqlType(typeName)
        } get
      }
    }
  }
  // ensure to use our customized postgres driver at 'import profile.api._'
  override def packageCode(profile: String, pkg: String, container: String, parentType: Option[String]): String = {
    s"""
package ${pkg}
// AUTO-GENERATED Slick data model

/** Stand-alone Slick data model for immediate use */
object ${container} extends {
  val profile = ${profile}
} with ${container}

/** Slick data model trait for extension (cake pattern). (Make sure to initialize this late.) */
trait ${container}${parentType.map(t => s" extends $t").getOrElse("")} {
  val profile: $profile  // must retain this driver
  import profile.api._
  
  ${indent(code)}
}
// auto generated type-safe primary key value classes
${tsPkDefsCode.result.flatten.mkString}  
      """.trim()
  }
}

/*


If the word is only one letter, do nothing.

If the word ends in "as", "is" or "us", do nothing and return the word.
mitosis -> mitosis

veritas -> veritas

syllabus -> syllabus

If the word ends in "ss", do nothing and return the word.
helpless -> helpless

If the word ends in "sses", change this to "s" and return the stem.
bosses -> bos

Similarly, "xes" -> "x"
boxes -> box

"zes" -> "z"
mazes -> maz

"ches" -> "ch"
porches -> porch

"shes" -> "sh"
brushes -> brush

"eys" -> "ey"
monkeys -> monkey

"ies" -> "y"
parties -> party

"s" -> ""
rulers -> ruler

boss -> bos

function depluralize($word){
    // Here is the list of rules. To add a scenario,
    // Add the plural ending as the key and the singular
    // ending as the value for that key. This could be
    // turned into a preg_replace and probably will be
    // eventually, but for now, this is what it is.
    //
    // Note: The first rule has a value of false since
    // we don't want to mess with words that end with
    // double 's'. We normally wouldn't have to create
    // rules for words we don't want to mess with, but
    // the last rule (s) would catch double (ss) words
    // if we didn't stop before it got to that rule. 
    $rules = array( 
        'ss' => false, 
        'os' => 'o', 
        'ies' => 'y', 
        'xes' => 'x', 
        'oes' => 'o', 
        'ies' => 'y', 
        'ves' => 'f', 
        's' => '');
    // Loop through all the rules and do the replacement. 
    foreach(array_keys($rules) as $key){
        // If the end of the word doesn't match the key,
        // it's not a candidate for replacement. Move on
        // to the next plural ending. 
        if(substr($word, (strlen($key) * -1)) != $key) 
            continue;
        // If the value of the key is false, stop looping
        // and return the original version of the word. 
        if($key === false) 
            return $word;
        // We've made it this far, so we can do the
        // replacement. 
        return substr($word, 0, strlen($word) - strlen($key)) . $rules[$key]; 
    }
    return $word;
}

 */
  