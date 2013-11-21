#include <microframework/api/utils/ModelVisitor.h>

void ModelVisitor::stopVisit()
{
        visitStopped = true;
}

void ModelVisitor::noChildrenVisit()
{
        visitChildren = true;
}


ModelVisitor::ModelVisitor(){
	  visitStopped = false;
	  visitChildren = true;
	  visitReferences=true;
	  //alreadyVisited.set_empty_key("");
}


void ModelVisitor::noReferencesVisit(){
     visitReferences = false;
}