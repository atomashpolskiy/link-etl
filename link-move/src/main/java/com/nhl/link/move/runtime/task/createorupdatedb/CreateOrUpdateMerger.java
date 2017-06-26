package com.nhl.link.move.runtime.task.createorupdatedb;

import com.nhl.link.move.LmRuntimeException;
import com.nhl.link.move.mapper.Mapper;
import com.nhl.link.move.runtime.task.createorupdate.CreateOrUpdateTuple;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @since 1.3
 */
public class CreateOrUpdateMerger {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrUpdateMerger.class);

	private Mapper mapper;

	public CreateOrUpdateMerger(Mapper mapper) {
		this.mapper = mapper;
	}

	public void merge(List<CreateOrUpdateTuple<DataRow>> mapped) {
		for (CreateOrUpdateTuple<DataRow> t : mapped) {
			if (!t.isCreated()) {
				merge(t.getSource(), t.getTarget());
			}
		}
	}

	public List<CreateOrUpdateTuple<DataRow>> map(ObjectContext context,
												  Map<Object, Map<String, Object>> mappedSources,
												  List<DataRow> matchedTargets) {

        // clone mappedSources as we are planning to truncate it in this method
		Map<Object, Map<String, Object>> localMappedSources = new HashMap<>(mappedSources);

		List<CreateOrUpdateTuple<DataRow>> result = new ArrayList<>();

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
				result.add(new CreateOrUpdateTuple<>(src, t, false));
			}
		}

		// everything that's left are new objects
		for (Map.Entry<Object, Map<String, Object>> e : localMappedSources.entrySet()) {

			DataRow t = create((DataContext) context, e.getValue());

			result.add(new CreateOrUpdateTuple<>(e.getValue(), t, true));
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

    protected DataRow create(DataContext context, Map<String, Object> source) {
		source = source.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().substring(3), Map.Entry::getValue));
		DataRow row = new DataRow(source);
//		DataObject object = context.objectFromDataRow("etl11t_temp", row);
//		object.setObjectId(new ObjectId("etl11t_temp", "id", source.get("id")));
//		context.registerNewObject(object);
		DataObject object = (DataObject) context.newObject("etl11t_temp");
		object.setObjectId(new ObjectId("etl11t_temp", "id", source.get("id")));
		context.registerNewObject(object);
//		object.getObjectId().getIdSnapshot().clear();
//		object.getObjectId().getIdSnapshot().put("id", source.get("id"));
		source.forEach((k, v) -> {
			if (!k.equals("id")) {
				object.writePropertyDirectly(k, v);
			}
		});

		return row;
	}

	private void merge(Map<String, Object> source, DataRow target) {
		// TODO: check for properties that are absent in the target
		target.putAll(source);
	}
}
