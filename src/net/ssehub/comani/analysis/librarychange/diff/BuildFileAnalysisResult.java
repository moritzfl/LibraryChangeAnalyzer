package net.ssehub.comani.analysis.librarychange.diff;

import java.util.List;
import java.util.StringJoiner;

public class BuildFileAnalysisResult {

	private String buildFile;

	@Override
	public String toString() {
		StringJoiner libraryChangeStringBuilder = new StringJoiner("[", ",\n", "]");
		libraryChanges.forEach(change -> libraryChangeStringBuilder.add(change.toString()));
		return "BuildFileAnalysisResult [buildFile=" + this.buildFile  +"libraryChanges=\n" + libraryChangeStringBuilder.toString().indent(4) + "\n]";
		
	}

	private List<LibraryEntry> libraryChanges;

	public BuildFileAnalysisResult(String buildFile, List<LibraryEntry> libraryChanges) {
		this.libraryChanges = libraryChanges;
	}

	public List<LibraryEntry> getLibraryChanges() {
		return libraryChanges;
	}

	public static class LibraryEntry {

		@Override
		public String toString() {
			return "LibraryEntry [previousLibrary=" + previousLibrary + ", currentLibrary=" + currentLibrary
					+ ", changeType=" + this.getLibraryChangeType() + "]";
		}

		public LibraryEntry(LibraryInformation previousLibrary, LibraryInformation currentLibrary) {
			this.previousLibrary = previousLibrary;
			this.currentLibrary = currentLibrary;
		}

		public LibraryInformation getPreviousLibrary() {
			return previousLibrary;
		}

		public LibraryInformation getCurrentLibrary() {
			return currentLibrary;
		}

		private LibraryInformation previousLibrary;
		private LibraryInformation currentLibrary;

		public static class LibraryInformation {

			@Override
			public String toString() {
				return "LibraryInformation [dependencyType=" + dependencyType + ", identifier=" + identifier
						+ ", group=" + group + ", version=" + version + ", dependencyType=" + dependencyType + "]";
			}

			private String dependencyType;
			private String identifier;
			private String group;
			private String version;

			public String getIdentifier() {
				return identifier;
			}

			public String getGroup() {
				return group;
			}

			public String getVersion() {
				return version;
			}

			public String getDependencyType() {
				return this.dependencyType;
			}

			public LibraryInformation(String dependencyType, String identifier, String group, String version) {
				super();
				this.dependencyType = dependencyType;
				this.identifier = identifier;
				this.group = group;
				this.version = version;
			}

			public boolean isSameLibraryInDifferentVersion(LibraryInformation other) {
				return other != null && this.identifier.equals(other.identifier) && this.group.equals(other.group)
						&& !this.version.equals(other.version);
			}

			public boolean isSameLibraryInSameVersion(LibraryInformation other) {
				return other != null && this.identifier.equals(other.identifier) && this.group.equals(other.group)
						&& this.version.equals(other.version);
			}

			public boolean isSameLibrary(LibraryInformation other) {
				return other != null && this.identifier.equals(other.identifier) && this.group.equals(other.group);
			}

			public boolean equals(Object other) {
				if (other == null || !(other instanceof LibraryEntry)) {
					return false;
				} else {
					LibraryInformation otherLibInfo = (LibraryInformation) other;
					return this.identifier.equals(otherLibInfo.identifier) && this.group.equals(otherLibInfo.group)
							&& this.version.equals(otherLibInfo.version)
							&& this.dependencyType.equals(otherLibInfo.dependencyType);
				}
			}

		}

		public LibraryChangeType getLibraryChangeType() {
			if (previousLibrary == null && currentLibrary != null) {
				return LibraryChangeType.ADDITION;
			} else if (previousLibrary != null && currentLibrary == null) {
				return LibraryChangeType.REMOVAL;
			} else if (previousLibrary != null && currentLibrary != null) {
				if (previousLibrary.isSameLibraryInSameVersion(currentLibrary)) {
					return LibraryChangeType.NO_CHANGE;
				} else if (previousLibrary.isSameLibraryInDifferentVersion(currentLibrary)) {
					return LibraryChangeType.VERSION_CHANGE;
				} else {
					return LibraryChangeType.REPLACEMENT;
				}
			} else {
				return LibraryChangeType.UNDEFINED;
			}

		}

		public enum LibraryChangeType {
			NO_CHANGE, VERSION_CHANGE, ADDITION, REMOVAL, REPLACEMENT, UNDEFINED
		}

	}

}
