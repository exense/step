/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.maven;

import org.apache.maven.artifact.Artifact;
import org.bson.types.ObjectId;
import org.mockito.Mockito;
import step.controller.multitenancy.Tenant;

import java.io.File;
import java.net.URISyntaxException;

public abstract class AbstractMojoTest {

	public static final String GROUP_ID = "my-group-id";
	public static final String ARTIFACT_ID = "step-functions-plugins-java-handler-test-3";
	public static final String VERSION_ID = "0.0.0-SNAPSHOT";

	public static final String TEST_INCLUDE_CLASSES = "a.b.c,d.e.f";
	public static final String TEST_EXCLUDE_CLASSES = "x.y.z";
	public static final String TEST_INCLUDE_ANNOTATIONS = "annotationA,annotationB";
	public static final String TEST_EXCLUDE_ANNOTATIONS = "annotationZ";

	protected static final Tenant TENANT_1 = createTenant1();
	protected static final Tenant TENANT_2 = createTenant2();

	protected Artifact createArtifactMock() throws URISyntaxException {
		Artifact mainArtifact = Mockito.mock(Artifact.class);
		Mockito.when(mainArtifact.getArtifactId()).thenReturn(ARTIFACT_ID);
		Mockito.when(mainArtifact.getClassifier()).thenReturn("jar");
		Mockito.when(mainArtifact.getGroupId()).thenReturn(GROUP_ID);
		Mockito.when(mainArtifact.getVersion()).thenReturn(VERSION_ID);
		Mockito.when(mainArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar.jar").toURI()));
		return mainArtifact;
	}

	protected Artifact createArtifactWithDependenciesMock() throws URISyntaxException {
		Artifact jarWithDependenciesArtifact = Mockito.mock(Artifact.class);
		Mockito.when(jarWithDependenciesArtifact.getArtifactId()).thenReturn(ARTIFACT_ID);
		Mockito.when(jarWithDependenciesArtifact.getClassifier()).thenReturn("jar-with-dependencies");
		Mockito.when(jarWithDependenciesArtifact.getGroupId()).thenReturn(GROUP_ID);
		Mockito.when(jarWithDependenciesArtifact.getVersion()).thenReturn(VERSION_ID);
		Mockito.when(jarWithDependenciesArtifact.getFile()).thenReturn(new File(this.getClass().getClassLoader().getResource("step/plugins/maven/test-file-jar-with-dependencies.jar").toURI()));
		return jarWithDependenciesArtifact;
	}

	protected static Tenant createTenant1() {
		Tenant tenant1 = new Tenant();
		tenant1.setName("project1");
		tenant1.setProjectId(new ObjectId().toString());
		return tenant1;
	}

	protected static Tenant createTenant2() {
		Tenant tenant2 = new Tenant();
		tenant2.setName("project2");
		tenant2.setProjectId(new ObjectId().toString());
		return tenant2;
	}
}
