package com.nhl.link.move.runtime.task.createorupdatedb;

import org.apache.cayenne.DataRow;

import java.util.Map;

/**
 * @since 1.3
 */
public class CreateOrUpdateTuple {

	private DataRow target;
	private Map<String, Object> source;
	private boolean created;

	public CreateOrUpdateTuple(Map<String, Object> source, DataRow target, boolean created) {
		this.target = target;
		this.source = source;
		this.created = created;
	}

	public DataRow getTarget() {
		return target;
	}

	public Map<String, Object> getSource() {
		return source;
	}

	public boolean isCreated() {
		return created;
	}
}
