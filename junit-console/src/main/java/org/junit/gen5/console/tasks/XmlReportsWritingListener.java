/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.console.tasks;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;

import javax.xml.stream.XMLStreamException;

import org.junit.gen5.engine.TestExecutionResult;
import org.junit.gen5.launcher.TestExecutionListener;
import org.junit.gen5.launcher.TestIdentifier;
import org.junit.gen5.launcher.TestPlan;

class XmlReportsWritingListener implements TestExecutionListener {

	private final Path reportsDir;
	private final PrintWriter out;
	private final Clock clock;

	private XmlReportData reportData;

	public XmlReportsWritingListener(String reportsDir, PrintWriter out) {
		this(reportsDir, out, Clock.systemDefaultZone());
	}

	// For tests only
	XmlReportsWritingListener(String reportsDir, PrintWriter out, Clock clock) {
		this.reportsDir = Paths.get(reportsDir);
		this.out = out;
		this.clock = clock;
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		this.reportData = new XmlReportData(testPlan, clock);
		try {
			Files.createDirectories(reportsDir);
		}
		catch (IOException e) {
			printException("Could not create reports directory: " + reportsDir, e);
		}
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		this.reportData = null;
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		reportData.markSkipped(testIdentifier, reason);
		writeXmlReportInCaseOfRoot(testIdentifier);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		reportData.markStarted(testIdentifier);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
		reportData.markFinished(testIdentifier, result);
		writeXmlReportInCaseOfRoot(testIdentifier);
	}

	private void writeXmlReportInCaseOfRoot(TestIdentifier testIdentifier) {
		if (isARoot(testIdentifier)) {
			writeXmlReportSafely(testIdentifier);
		}
	}

	private void writeXmlReportSafely(TestIdentifier testIdentifier) {
		Path xmlFile = reportsDir.resolve("TEST-" + testIdentifier.getUniqueId() + ".xml");
		try (Writer fileWriter = Files.newBufferedWriter(xmlFile)) {
			new XmlReportWriter(reportData).writeXmlReport(testIdentifier, fileWriter);
		}
		catch (XMLStreamException | IOException e) {
			printException("Could not write XML report: " + xmlFile, e);
		}
	}

	private boolean isARoot(TestIdentifier testIdentifier) {
		return !testIdentifier.getParentId().isPresent();
	}

	private void printException(String message, Exception exception) {
		out.println(message);
		exception.printStackTrace(out);
	}

}
