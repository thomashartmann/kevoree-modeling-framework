package org.kevoree.impl

import org.kevoree.*

class PortImpl() : PortInternal {
override internal var internal_eContainer : org.kevoree.container.KMFContainer? = null
override internal var internal_containmentRefName : String? = null
override internal var internal_unsetCmd : (()->Unit)? = null
override internal var internal_readOnlyElem : Boolean = false
override internal var internal_recursive_readOnlyElem : Boolean = false
override internal var _portTypeRef : org.kevoree.PortTypeRef? = null
}