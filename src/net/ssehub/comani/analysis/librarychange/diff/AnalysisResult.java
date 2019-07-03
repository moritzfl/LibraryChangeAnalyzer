package net.ssehub.comani.analysis.librarychange.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class AnalysisResult {

	@Override
	public String toString() {
		StringJoiner resultJoiner = new StringJoiner(",\n");
		results.forEach(result -> resultJoiner.add(result.toString()));
		return "AnalysisResult [id=" + id + ", results=[\n" + resultJoiner.toString().indent(4) + "  ]\n]";
	}



	private String id;
	private List<BuildFileAnalysisResult> results = new ArrayList<BuildFileAnalysisResult>();
	

	public void setCommitId(String id) {
		this.id = id;
		
	}
	
	public String getCommitId() {
		return id;
	}

	public List<BuildFileAnalysisResult> getResults() {
		return results;
	}



	public void addResult(BuildFileAnalysisResult buildFileResult) {
		this.results.add(buildFileResult);
		
	}
	
	

}
