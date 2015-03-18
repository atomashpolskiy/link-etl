package com.nhl.link.etl.task.createorupdate;

import java.util.List;

import com.nhl.link.etl.Execution;
import com.nhl.link.etl.load.LoadListener;

/**
 * A stateless thread-safe processor of a single batch segment of a
 * create-or-update ETL task.
 * 
 * @since 1.3
 */
public class CreateOrUpdateSegmentProcessor<T> {

	private Class<T> type;
	private RowConverter rowConverter;
	private SourceMapper<T> mapper;
	private TargetMatcher<T> matcher;
	private CreateOrUpdateMerger<T> merger;
	private List<LoadListener<T>> loadListeners;

	public CreateOrUpdateSegmentProcessor(Class<T> type, RowConverter rowConverter, SourceMapper<T> mapper,
			TargetMatcher<T> matcher, CreateOrUpdateMerger<T> merger, List<LoadListener<T>> loadListeners) {
		this.type = type;
		this.rowConverter = rowConverter;
		this.mapper = mapper;
		this.matcher = matcher;
		this.merger = merger;
		this.loadListeners = loadListeners;
	}

	public void process(Execution exec, CreateOrUpdateSegment<T> segment) {

		// execute create-or-update pipeline stages
		convert(segment);
		map(segment);
		match(segment);
		merge(exec, segment);
		commit(segment);
	}

	private void convert(CreateOrUpdateSegment<T> segment) {
		segment.setTranslatedSources(rowConverter.convert(segment.getRows()));
	}

	private void map(CreateOrUpdateSegment<T> segment) {
		segment.setMappedSources(mapper.map(segment.getTranslatedSources()));
	}

	private void match(CreateOrUpdateSegment<T> segment) {
		segment.setMatchedTargets(matcher.match(type, segment.getContext(), segment.getMappedSources()));
	}

	private void merge(final Execution exec, CreateOrUpdateSegment<T> segment) {
		segment.setMerged(merger.merge(type, segment.getContext(), segment.getMappedSources(),
				segment.getMatchedTargets()));

		// dispatch post-merge events
		if (!loadListeners.isEmpty()) {
			for (CreateOrUpdateTuple<T> t : segment.getMerged()) {

				if (t.isCreated()) {
					for (LoadListener<T> l : loadListeners) {
						l.targetCreated(exec, t.getSource(), t.getTarget());
					}
				} else {
					for (LoadListener<T> l : loadListeners) {
						l.targetUpdated(exec, t.getSource(), t.getTarget());
					}
				}
			}
		}
	}

	private void commit(CreateOrUpdateSegment<T> segment) {
		segment.getContext().commitChanges();
	}

}