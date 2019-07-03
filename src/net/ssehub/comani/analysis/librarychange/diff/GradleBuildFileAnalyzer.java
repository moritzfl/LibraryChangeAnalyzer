package net.ssehub.comani.analysis.librarychange.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.comani.analysis.librarychange.diff.BuildFileAnalysisResult.LibraryEntry;
import net.ssehub.comani.analysis.librarychange.diff.BuildFileAnalysisResult.LibraryEntry.LibraryInformation;
import net.ssehub.comani.data.ChangedArtifact;

public class GradleBuildFileAnalyzer extends BuildFileAnalyzer {

	private static final String DEPENDENCIES_START = "\\s*dependencies\\s*\\{\\s*";
	private static final String SIMPLE_DEPENDENCY_STRING = "\\s*(?<dependencytype>\\w+)\\s+'(?<group>\\S+):(?<identifier>\\S+):(?<version>\\S+)'";
	private ChangedArtifact artifact;

	public GradleBuildFileAnalyzer(ChangedArtifact artifact) {
		this.artifact = artifact;

	}

	private List<String> reduceLines(List<String> lines, boolean afterCommit) {
		List<String> linesforConsideration = new ArrayList<String>(lines);
		// Filter to only incluede lines that
		if (afterCommit) {
			for (int i = 0; i < linesforConsideration.size(); i++) {
				String relevantLine = linesforConsideration.get(i);
				if (relevantLine.startsWith("-")) {
					linesforConsideration.remove(i);
					i--;
				}
			}
		} else {
			for (int i = 0; i < linesforConsideration.size(); i++) {
				String relevantLine = linesforConsideration.get(i);
				if (relevantLine.startsWith("+")) {
					linesforConsideration.remove(i);
					i--;
				}
			}
		}
		return linesforConsideration;
	}

	private List<String> getDependencyLines(List<String> lines, boolean afterCommit) {
		Stack<Character> stack = new Stack<Character>();
		Pattern pattern = Pattern.compile(DEPENDENCIES_START);
		List<String> dependencyLines = new ArrayList<String>();
		List<String> linesForConsideration = reduceLines(lines, afterCommit);

		// find dependencies
		for (String line : linesForConsideration) {
			// if the stack is empty, try to find the start of the dependency section
			if (stack.isEmpty()) {
				Matcher matcher = pattern.matcher(line);
				if (matcher.find()) {
					stack.push('{');
				}
			} else {
				StringBuilder dependencyLineBuilder = new StringBuilder();
				// Otherwise find the end of the dependency section
				for (int i = 0; i < line.length(); i++) {

					if (line.charAt(i) == '}') {
						stack.pop();
					} else if (line.charAt(i) == '{') {
						stack.push('{');
					}
					if (stack.isEmpty()) {
						break;
					} else {
						dependencyLineBuilder.append(line.charAt(i));
					}
				}
				dependencyLines.add(dependencyLineBuilder.toString());
			}
		}
		return dependencyLines;
	}

	private List<LibraryInformation> getLibraryEntryInformation(List<String> lines) {
		List<LibraryInformation> libInfos = new ArrayList<>();
		Pattern pattern = Pattern.compile(SIMPLE_DEPENDENCY_STRING);
		for (String line : lines) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.find()) {
				libInfos.add(new LibraryInformation(matcher.group("dependencytype"), matcher.group("group"),
						matcher.group("identifier"), matcher.group("version")));
			}
		}
		return libInfos;

	}

	@Override
	public BuildFileAnalysisResult analyze() {
		List<String> buildFileLines = this.artifact.getContent();
		List<String> newLines = this.getDependencyLines(buildFileLines, true);
		List<String> oldLines = this.getDependencyLines(buildFileLines, false);

		Map<String, LibraryInformation> oldLibInfo = new HashMap<String, LibraryInformation>();
		Map<String, LibraryInformation> newLibInfo = new HashMap<String, LibraryInformation>();
		for (LibraryInformation info : getLibraryEntryInformation(oldLines)) {
			oldLibInfo.put(info.getGroup() + ":" + info.getIdentifier(), info);
		}
		for (LibraryInformation info : getLibraryEntryInformation(newLines)) {
			newLibInfo.put(info.getGroup() + ":" + info.getIdentifier(), info);
		}

		List<LibraryEntry> libraryChanges = new ArrayList<LibraryEntry>();

		for (Entry<String, LibraryInformation> entry : oldLibInfo.entrySet()) {
			String key = entry.getKey();
			if (newLibInfo.containsKey(key)) {
				libraryChanges.add(new LibraryEntry(entry.getValue(), newLibInfo.get(key)));
			} else {
				libraryChanges.add(new LibraryEntry(entry.getValue(), null));
			}
		}

		for (Entry<String, LibraryInformation> entry : newLibInfo.entrySet()) {
			String key = entry.getKey();
			if (!oldLibInfo.containsKey(key)) {
				libraryChanges.add(new LibraryEntry(null, entry.getValue()));
			}
		}

		return new BuildFileAnalysisResult(this.artifact.getArtifactPath(), libraryChanges);
	}

}
