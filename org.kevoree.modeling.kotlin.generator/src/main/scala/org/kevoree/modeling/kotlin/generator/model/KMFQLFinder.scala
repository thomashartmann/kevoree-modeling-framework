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
package org.kevoree.modeling.kotlin.generator.model

import java.io.PrintWriter
import org.eclipse.emf.ecore.EClass
import org.kevoree.modeling.kotlin.generator.{ProcessorHelper, GenerationContext}
import scala.collection.JavaConversions._


/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 07/02/13
 * Time: 19:09
 */
trait KMFQLFinder {

  def generateKMFQLMethods(pr: PrintWriter, cls: EClass, ctx: GenerationContext, pack: String) {
    pr.println("override fun internalGetKey() : String? {")

    pr.println("if(key_cache != null){")
    pr.println("return key_cache")
    pr.println("} else {")

    var first = true
    pr.print("key_cache = ")
    val idAttributes = cls.getEAllAttributes.filter(att => att.isID && !att.getName.equals("generated_KMF_ID"))

    if (idAttributes.size > 0) {

      pr.print("\"")
      //gets all IDs
      idAttributes.sortWith {
        (att1, att2) => att1.getName.toLowerCase < att2.getName.toLowerCase
      } //order alphabetically
        .foreach {
        att =>
          if (!first) {
            //pr.print("+\"/\"+")
            pr.print("/")
          }
          pr.print("$" + ProcessorHelper.getInstance().protectReservedWords(att.getName))
          first = false
      }
      pr.print("\"")

    } else {
      pr.print(" generated_KMF_ID")
    }

    pr.println()

    pr.println("}")
    pr.println("return key_cache")

    pr.println("}")

    //GENERATE findByID methods
    var generateReflexifMapper = false
    cls.getEAllReferences.foreach(ref => {
      if (ref.isMany) {
        generateReflexifMapper = true
        pr.println("override fun find" + ProcessorHelper.getInstance().protectReservedWords(ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)) + "ByID(key : String) : " + ProcessorHelper.getInstance().fqn(ctx, ref.getEReferenceType) + "? {")
        if (ctx.persistence) {

          if(ref.isContainment){
            pr.println("val resolved = _" + ref.getName + ".get(key)")
            pr.println("if(resolved==null){")
            pr.println("val originFactory = (this as org.kevoree.modeling.api.persistence.KMFContainerProxy).originFactory!!")
            pr.println("val result = relativeLookupFrom(this," + ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + ",key)")
            pr.println("return result as? " + ProcessorHelper.getInstance().fqn(ctx, ref.getEReferenceType))
            pr.println("} else {")
            pr.println("return resolved")
            pr.println("}")
          } else {
            pr.println("checkLazyLoad()")
            pr.println("return " + "_" + ref.getName + ".get(key)")
          }


        } else {
          pr.println("return " + "_" + ref.getName + ".get(key)")
        }
        pr.println("}")
      }
    })

    generateFindByPathMethods(ctx, cls, pr)
  }

  def generateFindByPathMethods(ctx: GenerationContext, cls: EClass, pr: PrintWriter) {
    pr.print("override fun findByID(relationName:String,idP : String) : org.kevoree.modeling.api.KMFContainer? {")
    pr.println("when(relationName) {")
    cls.getEAllReferences.foreach(ref => {
      if (ref.isMany) {
        pr.println(ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + " -> {")
        pr.println("return find" + ProcessorHelper.getInstance().protectReservedWords(ref.getName.substring(0, 1).toUpperCase + ref.getName.substring(1)) + "ByID(idP)}")
      }
      if (ref.getUpperBound == 1) {
        pr.println(ProcessorHelper.getInstance().fqn(ctx, ctx.basePackageForUtilitiesGeneration) + ".util.Constants.Ref_" + ref.getName + " -> {")
        pr.println("val objFound = " + ProcessorHelper.getInstance().protectReservedWords(ref.getName))
        pr.println("if(objFound!=null && (objFound as " + ctx.kevoreeContainerImplFQN + ").internalGetKey() == idP){")
        pr.println("return objFound")
        pr.println("}else{return null}")
        pr.println("}")
      }
    })
    pr.println("else -> {return null}")
    pr.println("}")
    pr.println("}")
  }


}
