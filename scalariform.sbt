import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

// SbtScalariform.defaultScalariformSettings

SbtScalariform.scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignParameters, true)
  .setPreference(CompactStringConcatenation, true)
  .setPreference(CompactControlReadability, false)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
  .setPreference(SpacesWithinPatternBinders, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(SpacesAroundMultiImports, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)

  // #Scalariform formatter preferences
  // #Fri Apr 01 21:09:37 BST 2011
  // alignParameters=true
  // compactStringConcatenation=false
  // indentPackageBlocks=true
  // formatXml=true
  // preserveSpaceBeforeArguments=false
  // doubleIndentClassDeclaration=false
  // doubleIndentMethodDeclaration=false
  // rewriteArrowSymbols=false
  // alignSingleLineCaseStatements=true
  // alignSingleLineCaseStatements.maxArrowIndent=40
  // spaceBeforeColon=false
  // spaceInsideBrackets=false
  // spaceInsideParentheses=false
  // preserveDanglingCloseParenthesis=false
  // indentSpaces=2
  // indentLocalDefs=false
  // spacesWithinPatternBinders=true
  // spacesAroundMultiImports=true
