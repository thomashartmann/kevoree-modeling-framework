
import org.kevoree.ContainerRoot;
import org.kevoree.loader.XMIModelLoader;
import org.kevoree.serializer.JSONModelSerializer;
import org.kevoree.serializer.ModelSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 25/03/13
 * Time: 19:44
 */
public class HelloJson {

    public static void main(String[] args) throws IOException {
        System.out.println("HelloJSON");

        XMIModelLoader loader = new XMIModelLoader();
        ContainerRoot model = loader.loadModelFromPath(new File("/Users/duke/Documents/dev/dukeboard/kevoree-modeling-framework/samples/org.kevoree.modeling.sample.kevoree/src/test/resources/bootKloudNode1.kev")).get(0);

        System.out.println("ModelLoaded");

       // ModelSerializer saver = new ModelSerializer();
       // saver.serialize(model,System.out);

        File fp = File.createTempFile("jsonXMI","jsonXMI");
        FileOutputStream fop = new FileOutputStream(fp);

        ModelSerializer saver2 = new JSONModelSerializer();
        saver2.serialize(model,fop);

        fop.flush();
        fop.close();

        System.out.println(fp.getAbsolutePath());


    }


}