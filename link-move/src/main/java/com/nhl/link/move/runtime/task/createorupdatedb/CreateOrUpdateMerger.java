package com.nhl.link.move.runtime.task.createorupdatedb;

import com.nhl.link.move.LmRuntimeException;
import com.nhl.link.move.mapper.Mapper;
import org.apache.cayenne.DataRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @since 1.3
 */
public class CreateOrUpdateMerger {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrUpdateMerger.class);

	private Mapper mapper;

	public CreateOrUpdateMerger(Mapper mapper) {
		this.mapper = mapper;
	}

	public void merge(List<CreateOrUpdateTuple> mapped) {
		for (CreateOrUpdateTuple t : mapped) {
			if (!t.isCreated()) {
				merge(t.getSource(), t.getTarget());
			}
		}
	}

	public List<CreateOrUpdateTuple> map(Map<Object, Map<String, Object>> mappedSources,
										 List<DataRow> matchedTargets) {

        // clone mappedSources as we are planning to truncate it in this method
		Map<Object, Map<String, Object>> localMappedSources = new HashMap<>(mappedSources);

		List<CreateOrUpdateTuple> result = new ArrayList<>();

		for (DataRow t : matchedTargets) {

			// TODO: re-using method with appropriate parameter type; this is unexpected behavior for library user
			Object key = mapper.keyForSource(t);

			Map<String, Object> src = localMappedSources.remove(key);

			// a null can only mean some algorithm malfunction, as keys are all
			// coming from a known set of sources
			if (src == null) {
				throw new LmRuntimeException("Invalid key: " + key);
			}

			// skip phantom updates...
			if (willUpdate(src, t)) {
				result.add(new CreateOrUpdateTuple(src, t, false));
			}
		}

		// everything that's left are new objects
		for (Map.Entry<Object, Map<String, Object>> e : localMappedSources.entrySet()) {

			DataRow t = create(e.getValue());

			result.add(new CreateOrUpdateTuple(e.getValue(), t, true));
		}

		return result;
    }

    protected boolean willUpdate(Map<String, Object> source, DataRow target) {

		if (source.isEmpty()) {
			return false;
		}

		for (Map.Entry<String, Object> e : source.entrySet()) {
			// TODO: check for properties that are absent in the target
			String attribute = e.getKey();
			if (!target.containsKey(attribute) || !Objects.equals(e.getValue(), target.get(attribute))) {
                return true;
            }
		}

		return false;
	}

    protected DataRow create(Map<String, Object> source) {
		return new DataRow(source);
	}

	private void merge(Map<String, Object> source, DataRow target) {
		// TODO: check for properties that are absent in the target
		target.putAll(source);
	}
}
