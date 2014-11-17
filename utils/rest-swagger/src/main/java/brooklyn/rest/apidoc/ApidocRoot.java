/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.rest.apidoc;

import java.util.List;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonSetter;

import com.wordnik.swagger.core.Documentation;

@JsonIgnoreProperties({
    "com$wordnik$swagger$core$Documentation$$apis",
    "com$wordnik$swagger$core$Documentation$$models"
})
public class ApidocRoot extends Documentation {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @JsonProperty("apis")
    public List<ApidocEndpoint> getApidocApis() {
        return (List) getApis();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @JsonSetter("apis")
    public void setApidocApis(List<ApidocEndpoint> ep) {
        super.setApis((List)ep);
    }
    
}
