/*************************************************************************
 * Copyright (c) 2019, The Eclipse Foundation and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution, and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *************************************************************************/
package org.eclipse.dash.licenses.tests;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.dash.licenses.ContentId;
import org.eclipse.dash.licenses.LicenseData;
import org.eclipse.dash.licenses.review.GitLabReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GitLabReviewTests {

	@Nested
	class MavenReviewTests {
		private GitLabReview review;

		@BeforeEach
		void setup() {
			review = new GitLabReview(
					new LicenseData(ContentId.getContentId("maven/mavencentral/group.path/artifact/1.0")));
		}

		@Test
		void testTitle() {
			assertEquals("maven/mavencentral/group.path/artifact/1.0", review.getTitle());
		}

		@Test
		void testMavenSourceUrl() {
			assertEquals(
					"https://search.maven.org/remotecontent?filepath=group/path/artifact/1.0/artifact-1.0-sources.jar",
					review.getMavenSourceUrl());
		}
	}

	@Nested
	class NpmReviewTests {
		private GitLabReview review;

		@BeforeEach
		void setup() {
			review = new GitLabReview(new LicenseData(ContentId.getContentId("npm/npmjs/group.path/artifact/1.0")));
		}

		@Test
		void testTitle() {
			assertEquals("npm/npmjs/group.path/artifact/1.0", review.getTitle());
		}

		@Test
		@Disabled
		void testMavenSourceUrl() {
			assertNull(review.getMavenSourceUrl());
		}
	}
}
