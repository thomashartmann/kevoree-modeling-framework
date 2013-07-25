package org.kevoree.test.trace

import org.kevoree.container.KMFContainer

/**
 * Created by duke on 25/07/13.
 */

public class ModelApplicator(val targetModel: KMFContainer) {

    public fun applyTraceOnModel(traces: List<ModelTrace>) {
        for(trace in traces){

            if(trace is ModelAddTrace){
                val castedTrace = trace as ModelAddTrace

            }
            if(trace is ModelRemoveTrace){
                val castedTrace = trace as ModelRemoveTrace

            }
            if(trace is ModelRemoveAllTrace){
                val castedTrace = trace as ModelRemoveAllTrace

            }
            if(trace is ModelSetTrace){
                val castedTrace = trace as ModelSetTrace
                if(castedTrace.content != null){

                } else {

                }
            }
        }
    }


}