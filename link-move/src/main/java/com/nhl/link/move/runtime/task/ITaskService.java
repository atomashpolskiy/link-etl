package com.nhl.link.move.runtime.task;

import com.nhl.link.move.runtime.task.createorupdatedb.CreateOrUpdateDbBuilder;
import org.apache.cayenne.DataObject;

import com.nhl.link.move.CreateOrUpdateBuilder;
import com.nhl.link.move.DeleteBuilder;
import com.nhl.link.move.SourceKeysBuilder;

public interface ITaskService {

	/**
	 * Returns a builder of create-or-update ETL synchronization task.
	 * 
	 * @since 1.3
	 */
	<T extends DataObject> CreateOrUpdateBuilder<T> createOrUpdate(Class<T> type);

	CreateOrUpdateDbBuilder createOrUpdate(String dbEntityName);

	/**
	 * Returns a builder of target delete ETL synchronization task.
	 * 
	 * @since 1.3
	 */
	<T extends DataObject> DeleteBuilder<T> delete(Class<T> type);

	/**
	 * @since 1.3
	 */
	<T extends DataObject> SourceKeysBuilder extractSourceKeys(Class<T> type);

	/**
	 * @since 1.4
	 */
	SourceKeysBuilder extractSourceKeys(String targetEntityName);
}
