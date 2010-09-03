/*
 * Copyright 2002-2008 the original author or authors.
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
package iwein.samples.spring.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Iwein Fuld
 *         <p/>
 *         User: iwein
 *         Date: Aug 30, 2009
 *         Time: 7:58:39 PM
 */
@RunWith(org.springframework.test.context.junit4.SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ConfigurationIntegrationTest {

	public static TemporaryFolder parent = new TemporaryFolder();

	private String processId = ManagementFactory.getRuntimeMXBean().getName();

	@Autowired
	@Qualifier("in")
	public PollableChannel incomingChanges;

	@Autowired
	@Qualifier("out")
	public MessageChannel outgoingChanges;

	@Autowired
	public FileReadingMessageSource fileReader;

	@BeforeClass
	public static void injectConfig() throws IOException {
		parent.create();
		Config.directories.put("store", parent.getRoot().getPath());
	}

	@AfterClass
	public static void cleanup() {
		parent.delete();
	}

	@Test
	public void shouldParseContext() {
		// this test is be performed by the fixture (Spring)
	}

	@Test
	public void fileReaderShouldGetInputDir() {
		Object inputDir = new DirectFieldAccessor(fileReader).getPropertyValue("inputDirectory");
		assertThat(inputDir, is(notNullValue()));
	}

	@Test(timeout = 1000)
	public void newChangesArePickedUp() throws Exception {
		File file = parent.newFile(Long.toString(System.currentTimeMillis()) + processId);
		System.out.println(file);
		file.createNewFile();
		Message<?> received = incomingChanges.receive();
		assertThat(received, is(notNullValue()));
	}

	public static class Config {
		public static final Map<String, String> directories = new HashMap<String, String>();

		public static final String processId = ManagementFactory.getRuntimeMXBean().getName();
	}
}
