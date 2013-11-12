package org.kevoree.modeling.datastores.http

import org.kevoree.modeling.api.persistence.DataStore
import org.eclipse.jetty.server.handler.AbstractHandler
import org.eclipse.jetty.server.Request
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.server.Server

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 12/11/2013
 * Time: 15:06
 */

public class DataStoreHttpWrapper(val wrapped: DataStore, val port: Int) : AbstractHandler(), DataStore {

    override fun handle(target: String?, baseRequest: Request?, request: HttpServletRequest?, response: HttpServletResponse?) {
        response!!.setContentType("text/plain;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest!!.setHandled(true);
        val segmentName = target!!.substring(0, target.toString().indexOf("/"));
        if(HttpMethod.GET.`is`(request?.getMethod())){
            val result = get(segmentName, target)
            if(result != null){
                response.getWriter()!!.write(result)
            }
        } else {
            if(HttpMethod.PUT.`is`(request?.getMethod())){
                put(segmentName, target, baseRequest.getReader()!!.readText())
            } else {
                if(HttpMethod.DELETE.`is`(request?.getMethod())){
                    remove(segmentName, target)
                } else {
                    if(HttpMethod.POST.`is`(request?.getMethod())){
                        wrapped.sync()
                    } else {
                        response.sendError(404, "Not a standard ")
                    }
                }
            }
        }

    }

    var server: Server? = null

    fun startServer() {
        server = Server(8080)
        server!!.setHandler(this)
        server!!.start()
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    override fun get(segment: String, key: String): String? {
        return wrapped.get(segment, key)
    }
    override fun put(segment: String, key: String, value: String) {
        wrapped.put(segment, key, value)
    }
    override fun sync() {
        wrapped.sync()
    }
    override fun remove(segment: String, key: String) {
        wrapped.remove(segment, key)
    }


}
