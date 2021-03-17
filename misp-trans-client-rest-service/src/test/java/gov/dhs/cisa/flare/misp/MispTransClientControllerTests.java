/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.dhs.cisa.flare.misp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
//@WebMvcTest
public class MispTransClientControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Ignore
    @Test
    public void noParamEventShouldRunDefaultProcess() throws Exception {

        mockMvc.perform(get("/misptransclient")).andDo(print()).andExpect(status().isOk())
        	.andExpect(jsonPath("$.status").value("Success"));
    }
 
    @Ignore
    @Test
    public void stixToMispParamEventShouldRunSticToMispProcess() throws Exception {

        mockMvc.perform(get("/misptransclient?processType=stixToMisp")).andDo(print()).andExpect(status().isOk())
        	.andExpect(jsonPath("$.status").value("Success"));
    }
    
    @Ignore
    @Test
    public void xmlOutputParamEventShouldRunSticToMispProcess() throws Exception {
        mockMvc.perform(get("/misptransclient?processType=xmlOutput")).andDo(print()).andExpect(status().isOk());
    }
}