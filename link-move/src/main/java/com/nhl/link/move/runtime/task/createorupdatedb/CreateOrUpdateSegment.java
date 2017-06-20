package com.nhl.link.move.runtime.task.createorupdatedb;

import com.nhl.link.move.Row;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;

import java.util.List;
import java.util.Map;

public class CreateOrUpdateSegment {

	private ObjectContext context;
	private List<Row> sourceRows;

	private List<Map<String, Object>> sources;
	private Map<Object, Map<String, Object>> mappedSources;
	private List<DataRow> matchedTargets;
	private List<CreateOrUpdateTuple> merged;

	public CreateOrUpdateSegment(ObjectContext context, List<Row> rows) {
		this.sourceRows = rows;
		this.context = context;
	}

	public ObjectContext getContext() {
		return context;
	}

	public List<Row> getSourceRows() {
		return sourceRows;
	}

	public List<Map<String, Object>> getSources() {
		return sources;
	}

	public void setSources(List<Map<String, Object>> translatedSegment) {
		this.sources = translatedSegment;
	}

	public Map<Object, Map<String, Object>> getMappedSources() {
		return mappedSources;
	}

	public void setMappedSources(Map<Object, Map<String, Object>> mappedSegment) {
		this.mappedSources = mappedSegment;
	}

	public List<DataRow> getMatchedTargets() {
		return matchedTargets;
	}

	public void setMatchedTargets(List<DataRow> matchedTargets) {
		this.matchedTargets = matchedTargets;
	}

	public List<CreateOrUpdateTuple> getMerged() {
		return merged;
	}

	public void setMerged(List<CreateOrUpdateTuple> merged) {
		this.merged = merged;
	}

}
