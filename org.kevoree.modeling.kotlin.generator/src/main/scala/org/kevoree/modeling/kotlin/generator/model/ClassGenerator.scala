

package org.kevoree.modeling.kotlin.generator.model

import java.io.{File, PrintWriter}
import scala.collection.JavaConversions._
import org.eclipse.emf.ecore._
import org.kevoree.modeling.kotlin.generator.{ProcessorHelper, AspectMatcher, GenerationContext}
import scala.collection.mutable
import java.util

/**
 * Created by IntelliJ IDEA.
 * Users: Gregory NAIN, Fouquet Francois
 * Date: 23/09/11
 * Time: 13:35
 */

trait ClassGenerator extends ClonerGenerator with FlatReflexiveSetters {

  def toCamelCase(ref: EReference): String = {
    return ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)
  }

  def toCamelCase(att: EAttribute): String = {
    return att.getName.substring(0, 1).toUpperCase + att.getName.substring(1)
  }

  var param_suf = "P"

  def generateKMFQLMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String)

  //def generateSelectorMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext)

  //def generateContainedElementsMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext)

 // def generateDiffMethod(pr: PrintWriter, cls: EClass, ctx: GenerationContext)

  def generateFlatReflexiveSetters(ctx: GenerationContext, cls: EClass, pr: PrintWriter)

  def generateFlatClass(ctx: GenerationContext, currentPackageDir: String, packElement: EPackage, cls: EClass) {

    val localFile = new File(currentPackageDir + "/impl/" + cls.getName + "Impl.kt")
    val pr = new PrintWriter(localFile, "utf-8")
    val pack = ProcessorHelper.getInstance().fqn(ctx, packElement)
    pr.println("package " + pack + ".impl")
    pr.println()
    val aspects = ctx.aspects.values().filter(v => AspectMatcher.aspectMatcher(ctx, v, cls))
    var aspectsName = List[String]()
    aspects.foreach {
      a =>
        aspectsName = aspectsName ++ List(a.packageName + "." + a.name)
    }
    aspects.foreach {
      a =>
        pr.println("import " + a.packageName + ".*")
        if (ctx.js) {
          a.imports.filter(i => i != "org.kevoree.modeling.api.aspect" && i != "org.kevoree.modeling.api.meta").foreach {
            i =>
              pr.println("import " + i + ";")
          }
        }

    }


    pr.println(ProcessorHelper.getInstance().generateHeader(packElement))
    //case class name
    ctx.classFactoryMap.put(pack + "." + cls.getName, ctx.packageFactoryMap.get(pack))
    pr.print("class " + cls.getName + "Impl")

    val resultAspectName = if (!aspectsName.isEmpty && !ctx.newMetaClasses.exists(m => m.packageName + "." + m.name == ProcessorHelper.getInstance().fqn(ctx, cls))) {
      "," + aspectsName.mkString(",")
    } else {
      ""
    }

    pr.println(" : " + ctx.kevoreeContainerImplFQN + ", " + ProcessorHelper.getInstance().fqn(ctx, packElement) + "." + cls.getName + resultAspectName + " { ")

    pr.println("override internal var internal_eContainer : " + ctx.kevoreeContainer + "? = null")
    pr.println("override internal var internal_containmentRefName : String? = null")
    pr.println("override internal var internal_unsetCmd : " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand? = null")
    pr.println("override internal var internal_readOnlyElem : Boolean = false")
    pr.println("override internal var internal_recursive_readOnlyElem : Boolean = false")
    if (ctx.generateEvents) {
      pr.println("override internal var internal_modelElementListeners : MutableList<org.kevoree.modeling.api.events.ModelElementListener>? = null")
      pr.println("override internal var internal_modelTreeListeners : MutableList<org.kevoree.modeling.api.events.ModelElementListener>? = null")
    }
    if (ctx.persistence) {
      pr.println("override var isResolved: Boolean = true")
      pr.println("override var inResolution: Boolean = false")
      pr.println("override var originFactory: org.kevoree.modeling.api.persistence.PersistenceKMFFactory? = null")
    }
    if (ctx.timeAware) {
      pr.println("override var now: org.kevoree.modeling.api.time.TimePoint? = null")
      pr.println("override var previousTimePoint: org.kevoree.modeling.api.time.TimePoint? = null")
    }

    pr.println("override var path_cache : String? = null")
    pr.println("override var key_cache: String? = null")


    generateDeleteMethod(pr, cls, ctx, pack)
    generateAllGetterSetterMethod(pr, cls, ctx, pack)
    generateFlatReflexiveSetters(ctx, cls, pr)
    generateKMFQLMethods(pr, cls, ctx, pack)
    if (ctx.genSelector) {
      KMFQLSelectorGenerator.generateSelectorMethods(pr, cls, ctx)
    }
    ContainedElementsGenerator.generateContainedElementsMethods(pr, cls, ctx)
    generateMetaClassName(pr, cls, ctx)
    //Kotlin workaround // Why prop are not generated properly ?
    if (ctx.js && ctx.ecma3compat) {
      ProcessorHelper.getInstance().noduplicate(cls.getEAllAttributes).foreach {
        att =>
          if (att.isMany) {
            pr.println("override public fun get" + toCamelCase(att) + "()" + " : List<" + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + ">" + "{ return " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + "}")
            pr.println("override public fun set" + toCamelCase(att) + "(internal_p" + " : List<" + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + ">)" + "{ " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " = internal_p }")
          } else {
            pr.println("override public fun get" + toCamelCase(att) + "() : " + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + "? { return " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + "}")
            pr.println("override public fun set" + toCamelCase(att) + "(internal_p : " + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + "?) { " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " = internal_p }")
          }
      }
      ProcessorHelper.getInstance().noduplicateRef(cls.getEAllReferences).foreach {
        ref =>
          val typeRefName = ProcessorHelper.getInstance().fqn(ctx, ref.getEReferenceType)
          if (ref.isMany) {
            pr.println("override public fun get" + toCamelCase(ref) + "()" + " : List<" + typeRefName + ">" + "{ return " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + "}")
            pr.println("override public fun set" + toCamelCase(ref) + "(internal_p" + " : List<" + typeRefName + ">){ " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + " = internal_p }")
          } else {
            pr.println("override public fun get" + toCamelCase(ref) + "() : " + typeRefName + "?" + "{ return " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + "}")
            pr.println("override public fun set" + toCamelCase(ref) + "(internal_p : " + typeRefName + "?){ " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + " = internal_p }")
          }
      }
    }


    if (aspects.size > 1) {
      val methodUsage = new util.HashMap[String, java.util.List[String]]() //todo not only on method name
      aspects.foreach {
        aspect =>
          aspect.methods.foreach {
            method =>
              if (!methodUsage.containsKey(method.name)) {
                methodUsage.put(method.name, new util.ArrayList[String]())
              }
              methodUsage.get(method.name).add(aspect.packageName + "." + aspect.name)
          }
      }
      methodUsage.foreach {
        t =>
          if (t._2.size() > 1) {

            cls.getEAllOperations.find(eop => eop.getName == t._1) match {
              //better match
              case Some(op) => {
                pr.print("override fun " + op.getName + "(")
                var isFirst = true
                op.getEParameters.foreach {
                  p =>
                    if (!isFirst) {
                      pr.println(",")
                    }
                    val returnTypeP = if (p.getEType.isInstanceOf[EDataType]) {
                      ProcessorHelper.getInstance().convertType(p.getEType.getName)
                    } else {
                      ProcessorHelper.getInstance().fqn(ctx, p.getEType)
                    }
                    pr.print(p.getName() + "P :" + returnTypeP)
                    isFirst = false
                }
                if (op.getEType != null) {

                  var returnTypeOP = if (op.getEType.isInstanceOf[EDataType]) {
                    ProcessorHelper.getInstance().convertType(op.getEType.getName)
                  } else {
                    ProcessorHelper.getInstance().fqn(ctx, op.getEType)
                  }

                  if (op.getLowerBound == 0) {
                    returnTypeOP = returnTypeOP + "?"
                  }

                  pr.println("):" + returnTypeOP + "{")
                } else {
                  pr.println("):Unit{")
                }

                if (!ctx.js) {
                  var currentT = t._2.size()
                  t._2.foreach {
                    superTrait =>
                      currentT = currentT - 1
                      if (currentT == 0) {
                        pr.print("return ")
                      }
                      pr.print("super<" + superTrait + ">." + op.getName + "(")
                      var isFirst = true
                      op.getEParameters.foreach {
                        param =>
                          if (!isFirst) {
                            pr.println(",")
                          }
                          pr.print(param.getName + "P")
                          isFirst = false
                      }
                      pr.println(")")
                  }
                } else {
                  //JS generate plain method code inside method body

                  var currentT = t._2.size()
                  t._2.foreach {
                    superTrait =>
                      currentT = currentT - 1
                      val aspect = aspects.find(a => a.packageName + "." + a.name == superTrait).get
                      val method = aspect.methods.find(m => m.name == op.getName).get
                      if (currentT == 0) {
                        pr.println(aspect.getContent(method))
                      } else {
                        val content = aspect.getContent(method).trim
                        if (!content.startsWith("throw ")) {
                          pr.println(content.replace("return", ""))
                        }
                      }
                  }
                }
                pr.println("}")

              }
              case _ => {
                System.err.println("Not Found " + t._1)
              }
            }
          }
      }
    }

    val hashSetVar = mutable.HashSet[String]()
    aspects.foreach {
      aspect =>
        aspect.vars.foreach {
          varD =>
            if (!hashSetVar.contains(varD.name) && varD.isPrivate) {
              var initString = "null"
              if (!varD.typeName.trim.endsWith("?")) {
                initString = ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants." + varD.typeName.toUpperCase + "_DEFAULTVAL"
              }
              pr.println("override var " + varD.name + " : " + varD.typeName + " = " + initString)
              hashSetVar.add(varD.name)
            }
        }
    }

    pr.println("}")
    pr.flush()
    pr.close()
  }


  private def generateMetaClassName(pr: PrintWriter, cls: EClass, ctx: GenerationContext) {
    pr.println("override fun metaClassName() : String {")
    pr.println("return " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants." + ProcessorHelper.getInstance().fqn(ctx, cls).replace('.', '_') + ";")
    pr.println("}")
  }

  private def generateDeleteMethod(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String) {
    pr.println("override fun delete(){")
    pr.println("if(internal_unsetCmd!=null){internal_unsetCmd!!.run()}")
    if (ctx.persistence) {
      pr.println("(this as org.kevoree.modeling.api.persistence.KMFContainerProxy).originFactory!!.remove(this)")
    } else {
      cls.getEAllReferences.foreach {
        ref =>
          if (ref.isMany) {
            pr.println("removeAll"+toCamelCase(ref)+"()")
          } else {
            pr.println(ProcessorHelper.getInstance().protectReservedWords(ref.getName) + " = null")
          }
      }
    }
    pr.println("}")
  }


  private def generateAttributeSetterWithParameter(pr: PrintWriter, att: EAttribute, ctx: GenerationContext, pack: String, idAttributes: mutable.Buffer[EAttribute]) {

    if (att.isMany) {
      pr.println("\tprivate fun internal_" + att.getName + "(iP : List<" + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + ">?, fireEvents : Boolean = true){")
    } else {
      pr.println("\tprivate fun internal_" + att.getName + "(iP : " + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + "?, fireEvents : Boolean = true){")
    }
    pr.println("if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}")
    pr.println("if(iP != " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + "){")
    if (ctx.generateEvents) {
      pr.println("val oldPath = path()")
    }
    if (att.isID()) {
      pr.println("val oldId = internalGetKey()")
      if (ctx.persistence) {
        pr.println("if(!inResolution){")
      }
      pr.println("path_cache = null")
      pr.println("key_cache = null")
      if (ctx.persistence) {
        pr.println("}")
      }
      pr.println("val previousParent = eContainer();")
      pr.println("val previousRefNameInParent = getRefInParent();")
    }
    pr.println("val kmf_previousVal = $" + ProcessorHelper.getInstance().protectReservedWords(att.getName))
    pr.println("$" + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " = iP")
    if (ctx.generateEvents) {
      pr.println("if(fireEvents) {")
      pr.println("fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(oldPath, org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.ATTRIBUTE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Att_" + att.getName + ", " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + ",kmf_previousVal))")
      pr.println("}")
    }
    if (att.isID()) {
      pr.println("if(previousParent!=null){")
      pr.println("previousParent.reflexiveMutator(org.kevoree.modeling.api.util.ActionType.RENEW_INDEX, previousRefNameInParent!!, oldId,false,false);")
      pr.println("}")
      if (ctx.generateEvents) {
        pr.println("if(fireEvents) {")
        pr.println("fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(oldPath, org.kevoree.modeling.api.util.ActionType.RENEW_INDEX, org.kevoree.modeling.api.util.ElementAttributeType.ATTRIBUTE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Att_" + att.getName + ", path(),null))")
        pr.println("}")
      }

      pr.println("visit(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".container.cleanCacheVisitor,true,true,false);")

    }
    pr.println("\t}")
    pr.println("\t}//end of setter")
  }


  private def generateAllGetterSetterMethod(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String) {
    val idAttributes = cls.getEAllAttributes.filter(att => att.isID && !att.getName.equals("generated_KMF_ID"))
    val alreadyGeneratedAttributes = new mutable.HashSet[String]()
    cls.getEAllAttributes.foreach {
      att =>
        if (!alreadyGeneratedAttributes.contains(att.getName)) {
          alreadyGeneratedAttributes.add(att.getName)
          var defaultValue = ProcessorHelper.getInstance().getDefaultValue(ctx, att)
          if (att.getName.equals("generated_KMF_ID") && idAttributes.size == 0) {
            if (ctx.js) {
              defaultValue = "\"\"+Math.random() + java.util.Date().getTime()"
            } else {
              defaultValue = "\"\"+hashCode() + java.util.Date().getTime()"
            }
          } else {
            if (att.isMany) {
              defaultValue = "java.util.ArrayList<" + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + ">()"
            }
          }
          //Generate getter
          if (att.isMany) {
            if (defaultValue == null || defaultValue == "") {
              pr.println("public override var " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " : List<" + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + ">? = null")
            } else {
              pr.println("public override var " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " : List<" + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + ">? = " + defaultValue)
            }
            pr.println("\t set(iP : List<" + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + ">?){")
          } else {
            if (defaultValue == null || defaultValue == "") {
              pr.println("public override var " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " : " + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + "? = null")
            } else {
              pr.println("public override var " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " : " + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + "? = " + defaultValue)
            }
            pr.println("\t set(iP : " + ProcessorHelper.getInstance().convertType(att.getEAttributeType, ctx) + "?){")
          }
          if (ctx.persistence) {
            pr.println("checkLazyLoad()")
          }
          if (ctx.generateEvents) {
            pr.println("internal_" + att.getName + "(iP, true)")
          } else {
            pr.println("if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}")
            pr.println("if(iP != " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + "){")
            if (ctx.generateEvents) {
              pr.println("val oldPath = path()")
            }
            if (att.isID()) {
              pr.println("val oldId = internalGetKey()")
              pr.println("val previousParent = eContainer();")
              pr.println("val previousRefNameInParent = getRefInParent();")

              if (ctx.persistence) {
                pr.println("if(!inResolution){")
              }
              pr.println("path_cache = null")
              pr.println("key_cache = null")
              if (ctx.persistence) {
                pr.println("}")
              }

            }
            pr.println("val kmf_previousVal = $" + ProcessorHelper.getInstance().protectReservedWords(att.getName))
            pr.println("$" + ProcessorHelper.getInstance().protectReservedWords(att.getName) + " = iP")
            if (ctx.generateEvents) {
              pr.println("fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(oldPath, org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.ATTRIBUTE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Att_" + att.getName + ", " + ProcessorHelper.getInstance().protectReservedWords(att.getName) + ",kmf_previousVal))")
            }
            if (att.isID()) {
              pr.println("if(previousParent!=null){")
              pr.println("previousParent.reflexiveMutator(org.kevoree.modeling.api.util.ActionType.RENEW_INDEX, previousRefNameInParent!!, oldId,false,false);")
              pr.println("}")
              if (ctx.generateEvents) {
                pr.println("fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(oldPath, org.kevoree.modeling.api.util.ActionType.RENEW_INDEX, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Att_" + att.getName + ", path(),null))")
              }
            }
            pr.println("}")
          }
          pr.println("\t}//end of setter")

          if (ctx.persistence) {
            pr.println("get(){")
            pr.println("checkLazyLoad()")
            pr.println("return $" + ProcessorHelper.getInstance().protectReservedWords(att.getName))
            pr.println("}")
          }

          pr.println()
          if (ctx.generateEvents) {
            generateAttributeSetterWithParameter(pr, att, ctx, pack, idAttributes)
          }

        }
    }

    ProcessorHelper.getInstance().noduplicateRef(cls.getEAllReferences).foreach {
      ref =>
        val typeRefName = ProcessorHelper.getInstance().fqn(ctx, ref.getEReferenceType)
        if (ref.isMany) {
          //Declare internal cache (Hash Map)
          pr.println("internal val " + "_" + ref.getName + " : java.util.HashMap<String," + typeRefName + "> = java.util.HashMap<String," + typeRefName + ">()")
          pr.println("override var " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + ":List<" + ProcessorHelper.getInstance().fqn(ctx, ref.getEReferenceType) + ">");
          pr.println("\t  get(){")
          if (ctx.persistence) {
            pr.println("checkLazyLoad()")
          }
          pr.println("\t\t  return _" + ref.getName + ".values().toList()")
          pr.println("\t  }")
          pr.println(generateSetter(ctx, cls, ref, typeRefName, false))
          pr.println(generateAddMethod(cls, ref, typeRefName, ctx))
          pr.println(generateRemoveMethod(cls, ref, typeRefName, true, ctx))
        } else {
          pr.println("override var " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + ":" + ProcessorHelper.getInstance().fqn(ctx, ref.getEReferenceType) + "?=null");
          if (ctx.persistence) {
            pr.println("get(){")
            pr.println("checkLazyLoad()")
            pr.println("return $" + ProcessorHelper.getInstance().protectReservedWords(ref.getName))
            pr.println("}")
          }
          pr.println(generateSetter(ctx, cls, ref, typeRefName, true))
        }
    }
  }

  private def generateSetter(ctx: GenerationContext, cls: EClass, ref: EReference, typeRefName: String, isOptional: Boolean): String = {
    generateSetterOp(ctx, cls, ref, typeRefName) + generateInternalSetter(ctx, cls, ref, typeRefName)
  }

  private def generateInternalSetter(ctx: GenerationContext, cls: EClass, ref: EReference, typeRefName: String): String = {
    var res = "\nfun internal_" + ref.getName
    res += "(" + ref.getName + param_suf + " : "
    res += {
      if (ref.isMany) {
        "List<" + typeRefName + ">"
      } else {
        typeRefName + "?"
      }
    }
    res += ", setOpposite : Boolean, fireEvents : Boolean ) {\n"

    if (ctx.persistence) {
      res += ("checkLazyLoad()\n")
    }

    if (!ref.isMany) {
      res += "if($" + ref.getName + "!= " + ref.getName + param_suf + "){\n"
    } else {
      res += "if(_" + ref.getName + ".values()!= " + ref.getName + param_suf + "){\n"
    }

    if (!ref.isMany) {

      if (ref.getEOpposite != null) {
        res += "if(setOpposite) {\n"
        if (ref.getEOpposite.isMany) {
          // 0,1 or 1  -- *
          res += "if($" + ref.getName + " != null) {\n"
          res += "$" + ref.getName + "!!.reflexiveMutator(org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
          res += "}\n"
          res += "if(" + ref.getName + param_suf + "!=null) {\n"
          res += ref.getName + param_suf + ".reflexiveMutator(org.kevoree.modeling.api.util.ActionType.ADD, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
          res += "}\n"
        } else {
          // -> // 0,1 or 1  --  0,1 or 1
          res += "if($" + ref.getName + " != null){\n"
          res += "$" + ref.getName + "!!.reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", null, false, fireEvents)\n"
          res += "}\n"
          res += "if(" + ref.getName + param_suf + " != null){\n"
          res += ref.getName + param_suf + ".reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
          res += "}\n"
        }
        res += "}\n"
      }

      if (ref.isContainment) {
        // containment relation in noOpposite Method

        res += "if($" + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + " != null){\n"
        res += "($" + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + "!! as " + ctx.kevoreeContainerImplFQN + " ).setEContainer(null, null,null)\n"
        res += "}\n"

        res += "if(" + ref.getName + param_suf + "!=null){\n"
        if (ref.isMany) {
          res += "(" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").setEContainer(this, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ")," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ")\n"
        } else {
          if (ref.isRequired) {
            res += "(" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").setEContainer(this, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", null)," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ")\n"
          } else {
            res += "(" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + " ).setEContainer(this,null," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ")\n"
          }
        }
        res += "}\n"

      }

      //Setting of local reference

      res += "val kmf_previousVal = $" + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + "\n"

      res += "$" + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + " = " + ref.getName + param_suf + "\n"

    } else {
      // -> Collection ref : * or +

      res += "val kmf_previousVal = _" + ref.getName + "\n"

      if (ref.getEOpposite == null) {
        res += "_" + ref.getName + ".clear()\n"
      } else {
        res += "this.internal_removeAll" + toCamelCase(ref) + "(true, false)\n"
      }
      res += "for(el in " + ref.getName + param_suf + "){\n"
      res += "val elKey = (el as " + ctx.kevoreeContainerImplFQN + ").internalGetKey()\n"
      res += "if(elKey == null){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.ELEMENT_HAS_NO_KEY_IN_COLLECTION)}\n"
      res += "_" + ref.getName + ".put(elKey!!,el)\n"

      if (ref.isContainment) {
        if (ref.getEOpposite != null) {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").setEContainer(this," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", el)," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ")\n"
          if (ref.getEOpposite.isMany) {
            res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.ADD, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
          } else {
            res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
          }
        } else {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").setEContainer(this," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", el)," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ")\n"
        }
      } else {
        if (ref.getEOpposite != null) {
          if (ref.getEOpposite.isMany) {
            res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.ADD, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
          } else {
            res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
          }
        }
      }

      res += "}\n"
    }

    if (ctx.generateEvents) {
      res += "if(fireEvents) {\n"
      if (ref.isContainment) {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",kmf_previousVal))\n"
      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.SET, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",kmf_previousVal))\n"
      }
      res += "}\n"
    }

    res += "}\n" //END IF newRef != localRef
    res += "}\n"
    res
  }

  private def generateSetterOp(ctx: GenerationContext, cls: EClass, ref: EReference, typeRefName: String): String = {
    //generate setter
    var res = "\t set(" + ref.getName + param_suf + "){"

    //Read only protection
    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
    if (ref.isMany) {
      res += "if(" + ref.getName + param_suf + " == null){ throw IllegalArgumentException(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.LIST_PARAMETER_OF_SET_IS_NULL_EXCEPTION) }\n"
    }

    res += "internal_" + ref.getName + "(" + ref.getName + param_suf + ", true, true)"

    res += "\n}" //END Method
    res
  }


  private def generateAddMethod(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    generateDoAdd(cls, ref, typeRefName, ctx) +
      generateAdd(cls, ref, typeRefName, ctx) +
      generateAddAll(cls, ref, typeRefName, ctx) +
      (if (ref.getEOpposite != null || ctx.generateEvents) {
        generateAddWithParameter(cls, ref, typeRefName, ctx) +
          generateAddAllWithParameter(cls, ref, typeRefName, ctx)
      } else {
        ""
      })
  }

  private def generateDoAdd(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = ""
    res += "\nprivate fun doAdd" + toCamelCase(ref) + "(" + ref.getName + param_suf + " : " + typeRefName + ") {\n"

    res += "val _key_ = (" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").internalGetKey()\n"
    res += "if(_key_ == \"\" || _key_ == null){ throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.EMPTY_KEY) }\n"
    res += "if(!_" + ref.getName + ".containsKey(_key_)) {\n"

    res += "_" + ref.getName + ".put(_key_," + ref.getName + param_suf + ")\n"
    if (ref.isContainment) {
      res += "(" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").setEContainer(this," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".container.RemoveFromContainerCommand(this, org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ")," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ")\n"
    }

    res += "}\n"
    res += "}\n"
    res
  }

  private def generateAddWithParameter(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = ""
    res += "\nprivate fun internal_add" + toCamelCase(ref) + "(" + ref.getName + param_suf + " : " + typeRefName + ", setOpposite : Boolean, fireEvents : Boolean) {\n"

    if (ctx.persistence) {
      res += ("checkLazyLoad()\n")
    }

    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
    res += "doAdd" + toCamelCase(ref) + "(" + ref.getName + param_suf + ")\n"

    if (ref.getEOpposite != null) {
      res += "if(setOpposite){\n"
      val opposite = ref.getEOpposite
      if (!opposite.isMany) {
        res += "(" + ref.getName + param_suf + " as " + typeRefName + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
      } else {
        res += "(" + ref.getName + param_suf + " as " + typeRefName + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.ADD, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
      }
      res += "}\n"
    }

    if (ctx.generateEvents) {
      res += "if(fireEvents){\n"
      if (ref.isContainment) {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",null))\n"
      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",null))\n"
      }
      res += "}\n"
    }
    res += "}\n"
    res
  }

  private def generateAdd(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = ""
    res += "\noverride fun add" + toCamelCase(ref) + "(" + ref.getName + param_suf + " : " + typeRefName + ") {\n"

    if (ref.getEOpposite != null || ctx.generateEvents) {
      res += "internal_add" + toCamelCase(ref) + "(" + ref.getName + param_suf + ", true, true)\n"
    } else {
      res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
      res += "doAdd" + toCamelCase(ref) + "(" + ref.getName + param_suf + ")\n"

      if (ref.getEOpposite != null) {
        val opposite = ref.getEOpposite
        if (!opposite.isMany) {
          res += "(" + ref.getName + param_suf + " as " + typeRefName + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
        } else {
          res += "(" + ref.getName + param_suf + " as " + typeRefName + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.ADD, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
        }
      }
    }
    res += "}\n"
    res
  }

  private def generateAddAllWithParameter(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = ""
    res += "\nprivate fun internal_addAll" + toCamelCase(ref) + "(" + ref.getName + param_suf + " :List<" + typeRefName + ">, setOpposite : Boolean, fireEvents : Boolean) {\n"
    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
    res += "if (setOpposite) {\n"
    res += "for(el in " + ref.getName + param_suf + "){\n"
    res += "doAdd" + toCamelCase(ref) + "(el)\n"
    if (ref.getEOpposite != null) {
      val opposite = ref.getEOpposite
      if (!opposite.isMany) {
        res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
      } else {
        res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.ADD, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
      }
    }
    res += "}\n"
    res += "} else {\n"
    res += "for(el in " + ref.getName + param_suf + "){\n"
    res += "doAdd" + toCamelCase(ref) + "(el)\n"
    res += "}\n"
    res += "}\n"

    if (ctx.generateEvents) {
      res += "if (fireEvents) {\n"
      if (ref.isContainment) {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD_ALL, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",null))\n"
      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.ADD_ALL, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",null))\n"
      }
      res += "}\n"
    }
    res += "}\n"
    res
  }

  private def generateAddAll(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = ""
    res += "\noverride fun addAll" + toCamelCase(ref) + "(" + ref.getName + param_suf + " :List<" + typeRefName + ">) {\n"
    if (ref.getEOpposite != null || ctx.generateEvents) {
      res += "internal_addAll" + toCamelCase(ref) + "(" + ref.getName + param_suf + ", true, true)\n"
    } else {
      res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
      res += "for(el in " + ref.getName + param_suf + "){\n"
      res += "doAdd" + toCamelCase(ref) + "(el)\n"
      if (ref.getEOpposite != null) {
        val opposite = ref.getEOpposite
        if (!opposite.isMany) {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
        } else {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.ADD, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + opposite.getName + ", this, false, fireEvents)\n"
        }
      }
      res += "}\n"
    }
    res += "}\n"
    res
  }


  private def generateRemoveMethod(cls: EClass, ref: EReference, typeRefName: String, isOptional: Boolean, ctx: GenerationContext): String = {
    generateRemove(cls, ref, typeRefName, ctx) +
      generateRemoveAll(cls, ref, typeRefName, ctx) +
      (if (ref.getEOpposite != null || ctx.generateEvents) {
        generateRemoveMethodWithParam(cls, ref, typeRefName, ctx) +
          generateRemoveAllMethodWithParam(cls, ref, typeRefName, ctx)
      } else {
        ""
      })
  }


  private def generateRemoveMethodWithParam(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = "\nprivate fun internal_remove" + toCamelCase(ref) + "(" + ref.getName + param_suf + " : " + typeRefName + ", setOpposite : Boolean, fireEvents : Boolean) {\n"

    if (ctx.persistence) {
      res += ("checkLazyLoad()\n")
    }

    res += ("if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n")
    if (!ref.isRequired) {
      res += "if(" + "_" + ref.getName + ".size() != 0 && " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").internalGetKey())) {\n"
    } else {
      res += "if(" + "_" + ref.getName + ".size == " + ref.getLowerBound + "&& " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").internalGetKey()) ) {\n"
      res += "throw UnsupportedOperationException(\"The list of " + ref.getName + param_suf + " must contain at least " + ref.getLowerBound + " element. Can not remove sizeof(" + ref.getName + param_suf + ")=\"+" + "_" + ref.getName + ".size)\n"
      res += "} else {\n"
    }

    res += "_" + ref.getName + ".remove((" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").internalGetKey())\n"
    if (ref.isContainment) {
      //TODO
      res += "(" + ref.getName + param_suf + "!! as " + ctx.kevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
    }

    if (ctx.generateEvents) {
      if (ref.isContainment) {
        res += "if(!removeAll" + toCamelCase(ref) + "CurrentlyProcessing && fireEvents) {\n"
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",null))\n"
        res += "}\n"
      } else {
        res += "if(fireEvents) {\n"
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", " + ref.getName + param_suf + ",null))\n"
        res += "}\n"
      }
    }

    if (ref.getEOpposite != null) {
      res += "if(setOpposite){\n"
      if (ref.getEOpposite.isMany) {
        res += "(" + ref.getName + param_suf + " as " + typeRefName + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
      } else {
        res += "(" + ref.getName + param_suf + " as " + typeRefName + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", null, false, fireEvents)\n"
      }
      res += "}\n"
    }
    res += "}\n"
    res += "}\n"
    res
  }


  private def generateRemoveAllMethodWithParam(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = "\nprivate fun internal_removeAll" + toCamelCase(ref) + "(setOpposite : Boolean, fireEvents : Boolean) {\n"

    res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
    if (ctx.generateEvents && ref.isContainment) {
      res += "if(fireEvents){\n"
      res += "\nremoveAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "CurrentlyProcessing=true\n"
      res += "}\n"
    }
    res += "val temp_els = " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + "!!\n"

    if (ref.getEOpposite != null) {
      if (ref.isContainment) {
        res += "if(setOpposite){\n"
        res += "for(el in temp_els!!){\n"
        res += "(el as " + ctx.kevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
        if (!ref.getEOpposite.isMany) {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", null, false, fireEvents)\n"
        } else {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
        }
        res += "}\n"
        res += "} else {\n"
        res += "for(el in temp_els!!){\n"
        res += "(el as " + ctx.kevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
        res += "}\n"
        res += "}\n"
      } else {
        res += "if(setOpposite){\n"
        res += "for(el in temp_els!!){\n"
        if (!ref.getEOpposite.isMany) {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.SET, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", null, false, fireEvents)\n"
        } else {
          res += "(el as " + ctx.kevoreeContainerImplFQN + ").reflexiveMutator(org.kevoree.modeling.api.util.ActionType.REMOVE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getEOpposite.getName + ", this, false, fireEvents)\n"
        }
        res += "}\n"
        res += "}\n"
      }
    }


    res += "_" + ref.getName + ".clear()\n"

    if (ctx.generateEvents) {
      res += "if(fireEvents){\n"
      if (ref.isContainment) {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE_ALL, org.kevoree.modeling.api.util.ElementAttributeType.CONTAINMENT, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", temp_els,null))\n"
        res += "\nremoveAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "CurrentlyProcessing=false\n"
      } else {
        res += "fireModelEvent(org.kevoree.modeling.api.events.ModelEvent(path(), org.kevoree.modeling.api.util.ActionType.REMOVE_ALL, org.kevoree.modeling.api.util.ElementAttributeType.REFERENCE, " + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ", temp_els,null))\n"
      }
      res += "}\n"
    }

    res += "}\n"
    res
  }


  private def generateRemove(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = "\noverride fun remove" + toCamelCase(ref) + "(" + ref.getName + param_suf + " : " + typeRefName + ") {\n"
    if (ref.getEOpposite != null || ctx.generateEvents) {
      res += "internal_remove" + toCamelCase(ref) + "(" + ref.getName + param_suf + ", true, true)\n"
    } else {

      res += ("if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n")
      if (!ref.isRequired) {
        res += "if(" + "_" + ref.getName + ".size() != 0 && " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").internalGetKey())) {\n"
      } else {
        res += "if(" + "_" + ref.getName + ".size == " + ref.getLowerBound + "&& " + "_" + ref.getName + ".containsKey((" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").internalGetKey()) ) {\n"
        res += "throw UnsupportedOperationException(\"The list of " + ref.getName + param_suf + " must contain at least " + ref.getLowerBound + " element. Can not remove sizeof(" + ref.getName + param_suf + ")=\"+" + "_" + ref.getName + ".size)\n"
        res += "} else {\n"
      }

      res += "_" + ref.getName + ".remove((" + ref.getName + param_suf + " as " + ctx.kevoreeContainerImplFQN + ").internalGetKey())\n"
      if (ref.isContainment) {
        //TODO
        res += "(" + ref.getName + param_suf + "!! as " + ctx.kevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
      }
      res += "}\n"
    }
    res += "}\n"
    res
  }


  private def generateRemoveAll(cls: EClass, ref: EReference, typeRefName: String, ctx: GenerationContext): String = {
    var res = ""
    if (ctx.generateEvents && ref.isContainment) {
      // only once in the class, only for contained references
      res += "\nvar removeAll" + toCamelCase(ref) + "CurrentlyProcessing : Boolean = false\n"
    }
    res += "\noverride fun removeAll" + toCamelCase(ref) + "() {\n"
    if (ref.getEOpposite != null || ctx.generateEvents) {
      res += "internal_removeAll" + toCamelCase(ref) + "(true, true)\n"
    } else {
      res += "if(isReadOnly()){throw Exception(" + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.READ_ONLY_EXCEPTION)}\n"
      if (ctx.generateEvents && ref.isContainment) {
        res += "\nremoveAll" + ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1) + "CurrentlyProcessing=true\n"
      }
      if (ctx.generateEvents || ref.isContainment) {
        res += "val temp_els = " + ProcessorHelper.getInstance().protectReservedWords(ref.getName) + "!!\n"
      }
      if (ref.isContainment) {
        res += "for(el in temp_els!!){\n"
        res += "(el as " + ctx.kevoreeContainerImplFQN + ").setEContainer(null,null,null)\n"
        res += "}\n"
      }
      res += "_" + ref.getName + ".clear()\n"
    }
    res += "}\n"
    res
  }

}
