package com.nhl.link.move.runtime.task.createorupdatedb;

import com.nhl.link.move.Execution;
import com.nhl.link.move.annotation.AfterSourceRowsConverted;
import com.nhl.link.move.annotation.AfterSourcesMapped;
import com.nhl.link.move.annotation.AfterTargetsCommitted;
import com.nhl.link.move.annotation.AfterTargetsMapped;
import com.nhl.link.move.annotation.AfterTargetsMatched;
import com.nhl.link.move.annotation.AfterTargetsMerged;
import com.nhl.link.move.runtime.task.StageListener;
import com.nhl.link.move.runtime.task.createorupdate.SourceMapper;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * A stateless thread-safe processor for batch segments of a create-or-update
 * ETL task.
 */
public class CreateOrUpdateSegmentProcessor {

	private RowConverter rowConverter;
	private SourceMapper mapper;
	private TargetMatcher matcher;
	private CreateOrUpdateMerger merger;

	private Map<Class<? extends Annotation>, List<StageListener>> listeners;

	public CreateOrUpdateSegmentProcessor(RowConverter rowConverter,
										  SourceMapper mapper,
										  TargetMatcher matcher,
										  CreateOrUpdateMerger merger,
										  Map<Class<? extends Annotation>, List<StageListener>> stageListeners) {

		this.rowConverter = rowConverter;
		this.mapper = mapper;
		this.matcher = matcher;
		this.merger = merger;
		this.listeners = stageListeners;
	}

	public void process(Execution exec, CreateOrUpdateSegment segment) {

		// execute create-or-update pipeline stages
		convertSrc(exec, segment);
		mapSrc(exec, segment);
		matchTarget(exec, segment);
		mapToTarget(exec, segment);
		mergeToTarget(exec, segment);
		commitTarget(exec, segment);
	}

	private void convertSrc(Execution exec, CreateOrUpdateSegment segment) {
		segment.setSources(rowConverter.convert(segment.getSourceRows()));
		notifyListeners(AfterSourceRowsConverted.class, exec, segment);
	}

	private void mapSrc(Execution exec, CreateOrUpdateSegment segment) {
		segment.setMappedSources(mapper.map(segment.getSources()));
		notifyListeners(AfterSourcesMapped.class, exec, segment);
	}

	private void matchTarget(Execution exec, CreateOrUpdateSegment segment) {
		segment.setMatchedTargets(matcher.match(segment.getContext(), segment.getMappedSources()));
		notifyListeners(AfterTargetsMatched.class, exec, segment);
	}

	private void mapToTarget(Execution exec, CreateOrUpdateSegment segment) {
		segment.setMerged(merger.map(segment.getMappedSources(), segment.getMatchedTargets()));
		notifyListeners(AfterTargetsMapped.class, exec, segment);
	}

	private void mergeToTarget(Execution exec, CreateOrUpdateSegment segment) {
		merger.merge(segment.getMerged());
		notifyListeners(AfterTargetsMerged.class, exec, segment);
	}

	private void commitTarget(Execution exec, CreateOrUpdateSegment segment) {
		segment.getContext().commitChanges();
		notifyListeners(AfterTargetsCommitted.class, exec, segment);
	}

	private void notifyListeners(Class<? extends Annotation> type, Execution exec, CreateOrUpdateSegment segment) {
		List<StageListener> listenersOfType = listeners.get(type);
		if (listenersOfType != null) {
			for (StageListener l : listenersOfType) {
				l.afterStageFinished(exec, segment);
			}
		}
	}
}