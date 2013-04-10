package org.kevoree.impl

import org.kevoree.*

class ComponentInstanceImpl() : ComponentInstanceInternal {
override internal var internal_eContainer : org.kevoree.container.KMFContainer? = null
override internal var internal_containmentRefName : String? = null
override internal var internal_unsetCmd : (()->Unit)? = null
override internal var internal_readOnlyElem : Boolean = false
override internal var internal_recursive_readOnlyElem : Boolean = false
override internal var _name : String = ""
override internal var _metaData : String = ""
override internal var _typeDefinition : org.kevoree.TypeDefinition? = null
override internal var _dictionary : org.kevoree.Dictionary? = null
override internal var _provided_java_cache :List<org.kevoree.Port>? = null
override internal val _provided :MutableList<org.kevoree.Port> = java.util.ArrayList<org.kevoree.Port>()
override internal var _required_java_cache :List<org.kevoree.Port>? = null
override internal val _required :MutableList<org.kevoree.Port> = java.util.ArrayList<org.kevoree.Port>()
override internal var _namespace : org.kevoree.Namespace? = null
}