package org.kevoree.modeling.tests;

import org.junit.Test;
import org.kevoree.ContainerRoot;
import org.kevoree.TypedElement;
import org.kevoree.cloner.DefaultModelCloner;
import org.kevoree.compare.DefaultModelCompare;
import org.kevoree.impl.DefaultKevoreeFactory;
import org.kevoree.loader.XMIModelLoader;
import org.kevoree.modeling.api.ModelCloner;
import org.kevoree.modeling.api.compare.ModelCompare;
import org.kevoree.modeling.api.trace.ModelTrace;
import org.kevoree.modeling.api.trace.TraceSequence;
import org.kevoree.resolver.MavenResolver;
import org.kevoree.serializer.JSONModelSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created with IntelliJ IDEA.
 * User: leiko
 * Date: 8/26/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class MergeTest {

    @Test
    public void test() throws Exception {
        System.out.println("Testing merge");

        MavenResolver resolver = new MavenResolver();
        ArrayList<String> list = new ArrayList<String>();
        XMIModelLoader loader = new XMIModelLoader();
        ModelCompare compare = new DefaultModelCompare();
        DefaultKevoreeFactory factory = new DefaultKevoreeFactory();
        ContainerRoot fullModel = factory.createContainerRoot();

        list.add("http://oss.sonatype.org/content/groups/public");

        URL url = new URL("http://oss.sonatype.org/service/local/data_index?g=org.kevoree.corelibrary.javase");
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        NodeList nList = doc.getElementsByTagName("artifact");
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            NodeList childNode = node.getChildNodes();

            String resourceURI = null;
            String groupId = null;
            String artifactId = null;
            String version = null;
            String classifier = null;

            for (int j = 0; j < childNode.getLength(); j++) {
                Node nodeChild = childNode.item(j);
                if (nodeChild.getNodeName().endsWith("resourceURI")) {
                    resourceURI = nodeChild.getTextContent();
                }
                if (nodeChild.getNodeName().endsWith("groupId")) {
                    groupId = nodeChild.getTextContent();
                }
                if (nodeChild.getNodeName().endsWith("artifactId")) {
                    artifactId = nodeChild.getTextContent();
                }
                if (nodeChild.getNodeName().endsWith("version")) {
                    version = nodeChild.getTextContent();
                }
                if (nodeChild.getNodeName().endsWith("classifier")) {
                    classifier = nodeChild.getTextContent();
                }
            }
            if (resourceURI != null && !resourceURI.contains("-source") && classifier == null) {
                File file = resolver.resolve(groupId, artifactId, version, "jar", list);
                if (file != null) {
                    JarFile jar = new JarFile(file);
                    JarEntry jarEntry = jar.getJarEntry("KEV-INF/lib.kev");
                    if (jarEntry != null) {

                        System.err.println("Merge from " + file.getAbsolutePath());

                        ModelCloner cloner = new DefaultModelCloner();

                        ContainerRoot clonedModel = cloner.clone(fullModel);

                        ContainerRoot model = (ContainerRoot) loader.loadModelFromStream(jar.getInputStream(jarEntry)).get(0);
                        TraceSequence mergeSeq = compare.merge(fullModel, model);
                        // THIS IS GOING TO FAIL AT SOME POINT
                        // THROWING: java.lang.Exception: Unknown mutation type: 5

                        try {
                            mergeSeq.applyOn(fullModel);
                            JSONModelSerializer saver = new JSONModelSerializer();
                            saver.serialize(clonedModel, new ByteArrayOutputStream());
                        } catch (Exception e) {
                            e.printStackTrace();
                            JSONModelSerializer saver = new JSONModelSerializer();
                            File tempF = File.createTempFile("tempDebug", "tempDebug");
                            FileOutputStream tempout = new FileOutputStream(tempF);
                            saver.serialize(clonedModel, tempout);
                            tempout.flush();

                            File tempF2 = File.createTempFile("tempDebug2", "tempDebug2");
                            FileOutputStream tempout2 = new FileOutputStream(tempF2);
                            saver.serialize(model, tempout2);
                            tempout2.flush();

                            System.out.println("-"+model.findByPath("dataTypes[{Array[Byte]}]"));
                            System.out.println("--"+fullModel.findByPath("dataTypes[Array[Byte]]"));
                            System.out.println(model.findByID("dataTypes","Array[Byte]"));

                            System.err.println(">" + tempF.getAbsolutePath());
                            System.err.println(">>" + tempF2.getAbsolutePath());

                            System.err.println("Load from " + file.getAbsolutePath());

                            System.err.println("----------- Trace ");
                            for (ModelTrace t : mergeSeq.getTraces()) {
                                System.err.println(t);
                            }



                            return;
                        }

                    }
                }
            }
        }


    }
}
