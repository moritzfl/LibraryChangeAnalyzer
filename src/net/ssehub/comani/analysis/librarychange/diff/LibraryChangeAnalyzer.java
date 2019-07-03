/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.ssehub.comani.analysis.librarychange.diff;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Locale;

import net.ssehub.comani.core.Logger;
import net.ssehub.comani.core.Logger.MessageType;
import net.ssehub.comani.data.ChangedArtifact;
import net.ssehub.comani.data.Commit;

/**
 * This class represents a general diff analyzer, which checks the changes to
 * different types of artifacts with respect to their impact on the dead code
 * analysis. Hence, if the change may influence earlier results, the current
 * commit is marked accordingly.
 * 
 * @author Christian Kroeher
 *
 */
public class LibraryChangeAnalyzer {

	/**
	 * The string denoting the Java regular expression for identifying code files.
	 */
	private String codeFilePattern;

	/**
	 * The string denoting the Java regular expression for identifying build files.
	 */
	private String buildFilePattern;

	/**
	 * The {@link Commit} to analyze given via the constructor of this class.
	 * 
	 * @see #DiffAnalyzer(Commit)
	 */
	private Commit commit;

	/**
	 * The {@link AnalysisResult} of analyzing the {@link #commit}.
	 */
	private AnalysisResult analysisResult;

	/**
	 * Construct a new {@link LibraryChangeAnalyzer}.
	 * 
	 * @param vmFilesRegex    the regular expression identifying variability model
	 *                        files
	 * @param codeFilesRegex  the regular expression identifying code files
	 * @param buildFilesRegex the regular expression identifying build files
	 * @param commit          the {@link Commit} containing diff information
	 */
	public LibraryChangeAnalyzer(String codeFilesRegex, String buildFilesRegex, Commit commit) {

		this.codeFilePattern = codeFilesRegex;
		this.buildFilePattern = buildFilesRegex;
		this.commit = commit;
	}

	/**
	 * Analyze the artifacts changed by the given commit.
	 * 
	 * @return <code>true</code> if the analysis of the given commit (changed
	 *         artifacts) was successful, <code>false</code> otherwise
	 */
	public boolean analyze() {
		boolean analyzedSuccessful = false;
		try {
			if (!commit.getId().isEmpty()) {
				this.analysisResult = new AnalysisResult();
				this.analysisResult.setCommitId(this.commit.getId());
				List<ChangedArtifact> changedArtifactList = commit.getChangedArtifacts();
				for (ChangedArtifact artifact : changedArtifactList) {
					if (artifact.getArtifactPath().toLowerCase(Locale.ENGLISH).matches(this.buildFilePattern)) {
						Logger.getInstance().log(this.getClass().getName(), "processing",
								artifact.getArtifactPath() + " from commit " + commit.getId(), MessageType.DEBUG);
						BuildFileAnalyzer analyzer = BuildFileAnalyzerFactory.createBuildFileAnalyzer(artifact);
						BuildFileAnalysisResult buildFileResult = analyzer.analyze();
						this.analysisResult.addResult(buildFileResult);
					}
				}

				analyzedSuccessful = true;
			}
		} catch (Exception e) {
			Logger.getInstance().log(this.getClass().getName(), "Could not analyze commit " + commit.getId(),
					e.getMessage(), MessageType.ERROR);
		}
		return analyzedSuccessful;
	}

	/**
	 * Returns the result of analyzing the given commit.
	 * 
	 * @return the {@link #analysisResult}; may be <code>null</code>
	 */
	public AnalysisResult getResult() {
		return analysisResult;
	}

}
