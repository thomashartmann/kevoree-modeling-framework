/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * Fouquet Francois
 * Nain Gregory
 */
/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * Fouquet Francois
 * Nain Gregory
 */


package org.kevoree.modeling.kotlin.generator

import factories.FactoryGenerator
import java.io.{FileWriter, File}
import org.kevoree.modeling.kotlin.generator.loader.xml.XmiLoaderGenerator
import model.ModelGenerator
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore._
import org.kevoree.modeling.aspect.{AspectParam, AspectClass, AspectMethod}
import org.kevoree.modeling.kotlin.generator.events.EventsGenerator
import org.kevoree.modeling.kotlin.generator.loader.json.JsonLoaderGenerator
import org.kevoree.modeling.kotlin.generator.loader.LoaderApiGenerator
import scala.collection.JavaConversions._

import org.kevoree.modeling.kotlin.generator.serializer.{SerializerApiGenerator, SerializerJsonGenerator, SerializerGenerator}
import java.util

/**
 * Created by IntelliJ IDEA.
 * User: Gregory NAIN
 * Date: 21/09/11
 * Time: 23:05
 */

/**
 * Generator class. Proposes several methods for generation of Model, Loader, Serializer from a EMF-<i>ecore</i> model.
 * @param ctx the generation context
 * @param ecoreFile the ecore model that implementation will be generated
 */
class Generator(ctx: GenerationContext, ecoreFile: File) {
  preProcess()

  def preProcess() {
    val model = ctx.getEcoreModel(ecoreFile)

    //registering factories
    model.getAllContents.filter(e => e.isInstanceOf[EPackage]).foreach {
      elem => ctx.registerFactory(elem.asInstanceOf[EPackage])
    }
    ctx.setBaseLocationForUtilitiesGeneration(ecoreFile)
    model.getAllContents.foreach {
      content =>
        if (content.isInstanceOf[EPackage]) {
          val pack = content.asInstanceOf[EPackage]
          if (pack.getName == null || pack.getName == "") {
            throw new Exception("Package with an empty name : generation stopped !");
          }
        }
    }
  }


  def isMethodEquel(eop: EOperation, aop: AspectMethod, ctx: GenerationContext): Boolean = {
    if (eop.getName != aop.name) {
      return false
    }
    var methodReturnTypeTxt = ""
    if (eop.getEType != null) {
      if (eop.getEType.isInstanceOf[EDataType]) {
        methodReturnTypeTxt = ProcessorHelper.convertType(eop.getEType.getName)
      } else {
        methodReturnTypeTxt = ProcessorHelper.fqn(ctx, eop.getEType)
      }
    }

    var returnTypeCheck = false
    val equivalentMap = new java.util.HashMap[String, String]
    equivalentMap.put("List<*>", "MutableList");
    equivalentMap.put("List<Any?>", "MutableList");
    equivalentMap.put("MutableIterator<*>", "MutableIterator");
    equivalentMap.put("MutableIterator<Any?>", "MutableIterator");
    equivalentMap.put("Class<out jet.Any?>", "Class");
    if (equivalentMap.get(methodReturnTypeTxt) == aop.returnType || (eop.getEType != null && eop.getEType.getName == aop.returnType)) {
      returnTypeCheck = true
    } else {
      if (methodReturnTypeTxt == aop.returnType) {
        returnTypeCheck = true
      }
    }
    if (!returnTypeCheck) {
      System.err.println(methodReturnTypeTxt + "<->" + aop.returnType);
      return false
    }
    if (eop.getEParameters.size() != aop.params.size()) {
      //System.out.println(aop.name+"-"+eop.getEParameters.size()+"<?>"+aop.params.size());
      return false
    } else {
      var i = 0;
      eop.getEParameters.foreach {
        eparam =>
          i = i + 1
          val methodReturnTypeTxt = if (eparam.getEType.isInstanceOf[EDataType]) {
            ProcessorHelper.convertType(eparam.getEType.getName)
          } else {
            ProcessorHelper.fqn(ctx, eparam.getEType)
          }
          var returnTypeCheck = false
          if (equivalentMap.get(methodReturnTypeTxt) == aop.params.get(i - 1).`type` || (eparam.getEType != null && eparam.getEType.getName == aop.params.get(i - 1).`type`)) {
            returnTypeCheck = true
          } else {
            if (methodReturnTypeTxt == aop.params.get(i - 1).`type`) {
              returnTypeCheck = true
            }
          }

          if (!returnTypeCheck) {
            System.err.println(methodReturnTypeTxt + "<=>" + aop.params.get(i - 1).`type` + "/" + returnTypeCheck)
            return false
          }
      }
    }
    return true
  }


  /**
   * Triggers the generation of the given <i>ecore</i> file implementation.
   * @param modelVersion the version of the model (will be included in headers of generated files).
   */
  def generateModel(modelVersion: String) {

    val factoryGenerator = new FactoryGenerator(ctx)
    factoryGenerator.generateMainFactory()

    //if(ctx.generateEvents) {
    val eventsGenerator = new EventsGenerator(ctx)
    eventsGenerator.generateEvents()
    //}

    System.out.println("Check Aspect completeness")
    val model = ctx.getEcoreModel(ecoreFile)
    checkModel(model)
    //Create new metaClass
    ctx.newMetaClasses.foreach {
      metaC =>
        System.err.println("Auto creation of metaClass " + metaC.packageName + "." + metaC.name + " super " + metaC.parentName)
        val newMeta = EcoreFactory.eINSTANCE.createEClass();
        newMeta.setName(metaC.name);
        model.getResources.get(0).getContents.add(newMeta)
        model.getAllContents.find(e => e.isInstanceOf[EPackage] && ProcessorHelper.fqn(ctx, e.asInstanceOf[EPackage]) == metaC.packageName) match {
          case Some(p) => {
            p.asInstanceOf[EPackage].getEClassifiers.add(newMeta)
          }
          case None => {
            System.err.println("Create package : " + metaC.packageName);
            val newMetaPack = EcoreFactory.eINSTANCE.createEPackage();
            newMetaPack.setName(metaC.packageName)
            newMetaPack.getEClassifiers.add(newMeta)
            model.getResources.get(0).getContents.add(newMetaPack)
          }
        }
        model.getAllContents.find(e => e.isInstanceOf[EClass] && (ProcessorHelper.fqn(ctx, e.asInstanceOf[EClass]) == metaC.parentName || e.asInstanceOf[EClass].getName == metaC.parentName)) match {
          case Some(parentclass) => {
            newMeta.getESuperTypes.add(parentclass.asInstanceOf[EClass]);
          }
          case None => {
            throw new Exception("Parent Does not exist");
          }
        }


    }

    model.getAllContents.foreach {
      content =>
        content match {
          case eclass: EClass => {
            //Should have aspect covered all method
            val operationList = new util.ArrayList[EOperation]()
            operationList.addAll(eclass.getEAllOperations.filter(op => op.getName != "eContainer"))
            ctx.aspects.values().foreach {
              aspect =>
                if (AspectMatcher.aspectMatcher(ctx, aspect, eclass)) {
                  //aspect match
                  aspect.methods.foreach {
                    method =>
                      operationList.find(op => isMethodEquel(op, method, ctx)) match {
                        case Some(foundOp) => {
                          operationList.remove(foundOp)
                        }
                        case None => {
                          //is it a new method
                          if (!eclass.getEAllOperations.exists(op => isMethodEquel(op, method, ctx))) {

                            System.err.println("Add aspect Method to Ecore " + method.name + ":" + method.returnType + "/" + aspect.aspectedClass);

                            val newEOperation = EcoreFactory.eINSTANCE.createEOperation();
                            newEOperation.setName(method.name)

                            model.getAllContents.foreach {
                              c =>
                                if (c.isInstanceOf[EClass]) {
                                  val cc = c.asInstanceOf[EClass]
                                  if (cc.getName == method.returnType) { //TODO add FQN of class aspect
                                    newEOperation.setEType(cc)
                                  }
                                }
                            }
                            if (newEOperation.getEType == null) {
                              val dataType = EcoreFactory.eINSTANCE.createEDataType();
                              dataType.setName(method.returnType)
                              newEOperation.setEType(dataType)
                            }

                            method.params.foreach {
                              p =>
                                val newEParam = EcoreFactory.eINSTANCE.createEParameter();
                                newEParam.setName(p.name)
                                model.getAllContents.foreach {
                                  c =>
                                    if (c.isInstanceOf[EClass]) {
                                      val cc = c.asInstanceOf[EClass]
                                      if (cc.getName == p.`type`) { //TODO add FQN of class aspect
                                        newEParam.setEType(cc)
                                      }
                                    }
                                }
                                if (newEParam.getEType == null) {
                                  val dataTypeParam = EcoreFactory.eINSTANCE.createEDataType();
                                  dataTypeParam.setName(p.`type`)
                                  newEParam.setEType(dataTypeParam)
                                }
                                newEOperation.getEParameters.add(newEParam)
                            }
                            eclass.getEOperations.add(newEOperation)

                          } else {
                            eclass.getEAllOperations.filter(op => isMethodEquel(op, method, ctx)).foreach {
                              op =>
                                if (op.getEContainingClass == eclass) {
                                  var errMsg = "Duplicated Method In conflicting aspect on " + eclass.getName + "\n";
                                  errMsg = errMsg + method.toString
                                  throw new Exception(errMsg)
                                }
                            }

                          }
                        }
                      }
                  }
                }
            }
            if (!operationList.isEmpty && !eclass.isAbstract && !eclass.isInterface) {

              System.err.println("Auto generate Method for aspect " + eclass.getName);
              /*
              operationList.foreach {
                op =>
                  System.err.print(">" + op.getName)
                  op.getEParameters.foreach {
                    p =>
                      System.err.println("," + p.getName + ":" + p.getEType.getName);
                  }
                  System.err.println()
              } */

              val targetSrc = ctx.getRootSrcDirectory();
              val targetFile = new File(targetSrc + File.separator + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration).replace(".", File.separator) + File.separator + "GeneratedAspect_" + eclass.getName + ".kt");
              targetFile.getParentFile.mkdirs();
              val writer = new FileWriter(targetFile);
              writer.write("package " + ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration) + ";\n")
              writer.write("import org.kevoree.modeling.api.aspect;\n")
              writer.write("public aspect trait " + "GeneratedAspect_" + eclass.getName + " : " + ProcessorHelper.fqn(ctx, eclass) + " {\n")

              val newAspectClass = new AspectClass();
              newAspectClass.name = "GeneratedAspect_" + eclass.getName
              newAspectClass.aspectedClass = eclass.getName
              newAspectClass.packageName = ProcessorHelper.fqn(ctx, ctx.getBasePackageForUtilitiesGeneration)
              ctx.aspects.put(newAspectClass.packageName + "." + newAspectClass.name, newAspectClass)
              eclass.getEAllOperations.filter(op => op.getName != "eContainer").foreach {
                operation =>
                  writer.write("\toverride fun " + operation.getName + "(")
                  var isFirst = true
                  val newAspectOperation = new AspectMethod();
                  newAspectOperation.name = operation.getName
                  newAspectClass.methods.add(newAspectOperation)
                  operation.getEParameters.foreach {
                    param =>

                      val newParam = new AspectParam();
                      newParam.name = param.getName
                      newParam.`type` = ProcessorHelper.convertType(param.getEType.getName)
                      newAspectOperation.params.add(newParam)

                      if (!isFirst) {
                        writer.write(",")
                      }
                      if (param.getEType.isInstanceOf[EDataType]) {
                        writer.write("_" + param.getName + ":" + ProcessorHelper.convertType(param.getEType.getName))
                      } else {
                        if (param.getEType != null) {
                          writer.write("_" + param.getName + ":" + ProcessorHelper.fqn(ctx, param.getEType))
                        } else {
                          writer.write("_" + param.getName)
                        }
                      }
                      isFirst = false
                  }
                  if (operation.getEType != null) {
                    if (operation.getEType.isInstanceOf[EDataType]) {
                      var operationReturnType = ProcessorHelper.convertType(operation.getEType.getName)
                      if (operationReturnType.startsWith("List") && !ctx.getJS()) {
                        operationReturnType = "Mutable" + operationReturnType
                      }
                      writer.write(") : " + operationReturnType + " {\n")
                    } else {
                      val operationReturnType = ProcessorHelper.fqn(ctx, operation.getEType)
                      writer.write(") : " + operationReturnType + " {\n")
                    }
                  } else {
                    writer.write("){\n")
                  }
                  writer.write("\t\tthrow Exception(\"Not implemented yet !\");\n")
                  writer.write("\t}\n")
              }
              writer.write("}\n")
              writer.close()
              //create aspect to be able to be included by the factory


            }

          }
          case _ =>
        }
    }







    val modelGen = new ModelGenerator(ctx)
    modelGen.generateContainerAPI(ctx)
    modelGen.generateContainerTrait(ctx)
    modelGen.generateRemoveFromContainerCommand(ctx)


    System.out.println("Launching model generation")
    modelGen.process(model, modelVersion)

    System.out.println("Done with model generation")


  }

  def checkModel(model: ResourceSet) {

    model.getAllContents.foreach {
      content =>
        content match {
          case pack: EPackage => {
            if (pack.getNsPrefix == null || pack.getNsPrefix == "") {
              pack.setNsPrefix(pack.getName)
              System.err.println("The Metamodel package " + pack.getName + " does not have a Namespace Prefix. A namespace has been automatically used for generation.")
            }

            if (pack.getNsURI == null || pack.getNsURI == "") {
              pack.setNsURI("http://" + pack.getName)
              System.err.println("The Metamodel package " + pack.getName + " does not have a Namespace URI. A namespace has been automatically used for generation.")
              //throw new Exception("The base package "+pack.getName+" of the metamodel must contain an XML Namespace. Generation aborted.")
            }
          }
          case _ =>
        }
    }
  }


  private def checkOrGenerateLoaderApi() {
    val apiGenerator = new LoaderApiGenerator(ctx)
    apiGenerator.generateLoaderAPI()
  }

  def generateLoader() {

    checkOrGenerateLoaderApi()

    val model = ctx.getEcoreModel(ecoreFile)

    System.out.println("Launching loader generation")
    val loaderGenerator = new XmiLoaderGenerator(ctx)
    loaderGenerator.generateLoader(model)
    System.out.println("Done with loader generation")
  }

  def generateJsonLoader() {
    checkOrGenerateLoaderApi()
    val model = ctx.getEcoreModel(ecoreFile)
    System.out.println("Launching JSON loader generation")
    val loaderGenerator = new JsonLoaderGenerator(ctx)
    loaderGenerator.generateLoader(model)
    System.out.println("Done with JSON loader generation")
  }

  private def checkOrGenerateSerializerApi() {
    val apiGenerator = new SerializerApiGenerator(ctx)
    apiGenerator.generateSerializerAPI()
  }


  def generateSerializer() {

    checkOrGenerateSerializerApi()

    val model = ctx.getEcoreModel(ecoreFile)

    System.out.println("Launching serializer generation")
    val serializerGenerator = new SerializerGenerator(ctx)
    serializerGenerator.generateSerializer(model)
    System.out.println("Done with serializer generation")
  }

  def generateJSONSerializer() {

    checkOrGenerateSerializerApi()

    val model = ctx.getEcoreModel(ecoreFile)

    System.out.println("Launching json serializer generation")
    val serializerGenerator = new SerializerJsonGenerator(ctx)
    model.getAllContents.foreach {
      elem => elem match {
        case pack: EPackage => serializerGenerator.generateJsonSerializer(pack, model)
        case _ => //println("No serializer generator for containerRoot element of class: " + elem.getClass)
      }
    }
    System.out.println("Done with serializer generation")
  }

}