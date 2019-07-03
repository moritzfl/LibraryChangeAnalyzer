package net.ssehub.comani.analysis.librarychange.diff;

import net.ssehub.comani.data.ChangedArtifact;

public class BuildFileAnalyzerFactory {

	public static BuildFileAnalyzer createBuildFileAnalyzer(ChangedArtifact artifact) {
		if (artifact.getArtifactName().equalsIgnoreCase("pom.xml")) {
			//return new MavenBuildFileAnalyzer(artifact);
		} else if (artifact.getArtifactName().equalsIgnoreCase("build.gradle")) {
			return new GradleBuildFileAnalyzer(artifact);
		}
		return null;
	}



}
