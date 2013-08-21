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
package com.google.caliper.config;

import com.google.caliper.util.Util;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

// TODO(gak): remove this file (and the other CaliperRc) when we migrate to CaliperConfig
public final class CaliperRc {
  private final ImmutableMap<String, String> props;

  public CaliperRc(Map<String, String> props) {
    this.props = ImmutableMap.copyOf(props);
  }

  public String vmBaseDirectory() {
    return props.get("vm.baseDirectory");
  }

  public ImmutableMap<String, String> globalDefaultVmArgs() {
    return submap("vm.args.jdk"); // TODO: android etc.
  }

  public String homeDirForVm(String name) {
    return props.get("vm." + name + ".home");
  }

  public ImmutableMap<String, String> vmArgsForVm(String vmName) {
    return submap("vm." + vmName + ".args");
  }

  public List<String> verboseArgsForVm(String vmName) {
    String verboseArgs = props.get("vm." + vmName + ".verboseMode");
    if (verboseArgs == null) {
      return ImmutableList.of();
    } else {
      return ImmutableList.copyOf(Splitter.on(' ').split(verboseArgs));
    }
  }

  public String instrumentClassName(String instrumentName) {
    return props.get("instrument." + instrumentName + ".class");
  }

  public ImmutableMap<String, String> instrumentOptions(String instrumentName) {
    return submap("instrument." + instrumentName + ".options");
  }

  public ImmutableMap<String, String> vmArgsForInstrument(String instrumentName) {
    return submap("instrument." + instrumentName + ".vmArgs.jdk"); // TODO: android etc.
  }

  public String getProperty(String propertyName) {
    return props.get(propertyName);
  }

  public CaliperConfig asCaliperConfig() {
    return new CaliperConfig(props);
  }

  private ImmutableMap<String, String> submap(String name) {
    return Util.prefixedSubmap(props, name + ".");
  }

}