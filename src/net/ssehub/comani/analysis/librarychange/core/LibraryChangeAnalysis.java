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
package net.ssehub.comani.analysis.librarychange.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.ssehub.comani.analysis.AbstractCommitAnalyzer;
import net.ssehub.comani.analysis.AnalysisSetupException;
import net.ssehub.comani.analysis.librarychange.diff.AnalysisResult;
import net.ssehub.comani.analysis.librarychange.diff.LibraryChangeAnalyzer;
import net.ssehub.comani.core.Logger.MessageType;
import net.ssehub.comani.data.Commit;
import net.ssehub.comani.data.IAnalysisQueue;

/**
 * This class represents the main class of this analyzer.
 * 
 * @author Christian Kroeher
 *
 */
public class LibraryChangeAnalysis extends AbstractCommitAnalyzer {
    
    /**
     * The identifier of this class, e.g., for printing messages.
     */
    private static final String ID = "LibraryChangeAnalyzer";

    /**
     * The string representation of the properties' key identifying the regular expression for identifying code files.
     * The definition of this property is mandatory and has to define a valid Java regular expression.
     */
    private static final String PROPERTY_CODE_FILES_REGEX = "analysis.lybrary_change_analyzer.code_files_regex";
    
    /**
     * The string representation of the properties' key identifying the regular expression for identifying build files.
     * The definition of this property is mandatory and has to define a valid Java regular expression.
     */
    private static final String PROPERTY_BUILD_FILES_REGEX = "analysis.library_change_analyzer.build_files_regex";
    
    /**
     * The string representation of the properties' key identifying the output directory to which the analysis results
     * will be stored. The definition of this property is mandatory and has to define an existing directory.
     */
    private static final String PROPERTY_ANALYSIS_OUTPUT = "analysis.output";
    
    /**
     * The string denoting the Java regular expression for identifying code files. This value is set by
     * {@link #prepare()} based on the value of {@link #PROPERTY_CODE_FILES_REGEX}.
     */
    private String codeFilesRegex;
    
    /**
     * The string denoting the Java regular expression for identifying build files. This value is set by
     * {@link #prepare()} based on the value of {@link #PROPERTY_VM_FILES_REGEX}.
     */
    private String buildFilesRegex;
    
    /**
     * The results of the analysis in terms of the commit id (key) and their specific {@link AnalysisResult}s (value).
     */
    private HashMap<String, AnalysisResult> analysisResults;

	private Path outputPath;


    /**
     * Create a new instance of this analyzer.
     *  
     * @param analysisProperties the properties of the properties file defining the analysis process and the
     *        configuration of the analyzer in use; all properties, which start with the prefix "<tt>analysis.</tt>" 
     * @param commitQueue the {@link IAnalysisQueue} for transferring commits from an extractor to an analyzer
     * @throws AnalysisSetupException if the analyzer is not supporting the current operating or version control
     *         system as well as preparing this analyzer fails
     */
    public LibraryChangeAnalysis(Properties analysisProperties, IAnalysisQueue commitQueue)
            throws AnalysisSetupException {
        super(analysisProperties, commitQueue);
        prepare(); // throws exceptions if something is missing or not as expected
        logger.log(ID, this.getClass().getName() + " created", null, MessageType.DEBUG);
        outputPath = Paths.get(analysisProperties.getProperty(PROPERTY_ANALYSIS_OUTPUT));
    }
    
    /**
     * Prepares the analysis in terms of checking and adapting to properties.
     * 
     * @throws AnalysisSetupException if preparing fails  
     */
    private void prepare() throws AnalysisSetupException {
        codeFilesRegex = analysisProperties.getProperty(PROPERTY_CODE_FILES_REGEX);
        checkRegex(PROPERTY_CODE_FILES_REGEX, codeFilesRegex);
        buildFilesRegex = analysisProperties.getProperty(PROPERTY_BUILD_FILES_REGEX);
        checkRegex(PROPERTY_BUILD_FILES_REGEX, buildFilesRegex);
        // Second: initialize result map
        analysisResults = new HashMap<String, AnalysisResult>();
    }
    
    /**
     * Checks if the given regular expression (regex) for the given file identification property (regexProperty) is not
     * empty or undefined and a valid Java regular expression.
     * 
     * @param regexProperty one of {@link #PROPERTY_VM_FILES_REGEX}, {@link #PROPERTY_CODE_FILES_REGEX}, or
     *        {@link #PROPERTY_BUILD_FILES_REGEX}
     * @param regex the Java regular expression as defined by the user for one of the above properties
     * @throws AnalysisSetupException if the given expression is empty, undefined, or is not a valid Java regular
     *         expression
     */
    private void checkRegex(String regexProperty, String regex) throws AnalysisSetupException {
        String exceptionMessage = null;
        if (regex != null && !regex.isEmpty()) {
            try {                
                Pattern.compile(regex);
            } catch (PatternSyntaxException e) {
                exceptionMessage = "Regular expression for \"" + regexProperty + "\" is invalid: " + e.getDescription();
            }
        } else {
            exceptionMessage = "Missing Java regular expression for identifying files; please define \"" 
                    + regexProperty + "\" in the configuration file";
        }
        if (exceptionMessage != null) {
            throw new AnalysisSetupException(exceptionMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean analyze() {
        logger.log(ID, "Starting analysis", null, MessageType.DEBUG);
        LibraryChangeAnalyzer diffAnalyzer = null;
        boolean analysisSuccessful = false; // TODO: current check is incomplete
        while (commitQueue.isOpen()) {
            Commit commit = commitQueue.getCommit();
            if (commit != null) {
                logger.log(ID, "Analyzing commit " + commit.getId(), null, MessageType.DEBUG);
                diffAnalyzer = new LibraryChangeAnalyzer(codeFilesRegex, buildFilesRegex, commit);
                if (diffAnalyzer.analyze()) {
                    analysisResults.put(diffAnalyzer.getResult().getCommitId(), diffAnalyzer.getResult());
                    analysisSuccessful = true;
                    logger.log(ID, "Analysis of commit " + commit.getId() + " successful", null, MessageType.DEBUG);
                } else {
                    logger.log(ID, "Commit " + commit.getId() + " not analyzed", null, MessageType.DEBUG);
                }
            }
        }
        for (Entry<String, AnalysisResult> key: this.analysisResults.entrySet()) {
        	try {
        		((outputPath.resolve(key.getKey() + ".changedlibs.result")).toFile()).createNewFile();
				Files.write(outputPath.resolve(key.getKey() + ".changedlibs.result"), key.getValue().toString().getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				logger.log(this.getClass().getName(), "Could not write result", "Could not write result for commit " + key.getKey() + "  to target file " + outputPath.resolve(key.getKey() +  ".changedlibs.result" + " - Stacktrace: " + e.getMessage()), MessageType.ERROR);
			}
        }
        return analysisSuccessful;
    }
    

	/**
     * Returns the analysis results.
     * 
     * @return the {@link #analysisResults}
     */
    public HashMap<String, AnalysisResult> getResults() {
        return analysisResults;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean operatingSystemSupported(String operatingSystem) {
        // This analyzer is OS-independent
        logger.log(ID, "Supported operating systems: all", null, MessageType.DEBUG);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean versionControlSystemSupported(String versionControlSystem) {
        logger.log(ID, "Supported version control systems: Git and SVN", null, MessageType.DEBUG);
        return versionControlSystem.equalsIgnoreCase("Git") || versionControlSystem.equalsIgnoreCase("SVN");
    }

}
