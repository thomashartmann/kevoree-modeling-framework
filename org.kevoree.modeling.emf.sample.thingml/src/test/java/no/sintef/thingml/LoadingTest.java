/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors:
 * 	Fouquet Francois
 * 	Nain Gregory
 */
package no.sintef.thingml;/*
* Author : Gregory Nain (developer.name@uni.lu)
* Date : 29/01/13
* (c) 2013 University of Luxembourg – Interdisciplinary Centre for Security Reliability and Trust (SnT)
* All rights reserved
*/

import org.junit.Test;
import thingml.loader.ModelLoader;

import java.io.File;
import java.net.URISyntaxException;

public class LoadingTest {


    @Test
    public void loadingHelloWord() {

        try {

            ModelLoader loader = new ModelLoader();
            loader.loadModelFromPath(new File(getClass().getClassLoader().getResource("helloworld.xmi").toURI()));


        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }


}