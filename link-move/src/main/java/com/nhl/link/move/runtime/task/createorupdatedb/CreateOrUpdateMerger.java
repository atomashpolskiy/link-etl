package com.nhl.link.move.runtime.task.createorupdatedb;

import com.nhl.link.move.LmRuntimeException;
import com.nhl.link.move.mapper.Mapper;
import com.nhl.link.move.runtime.task.createorupdate.CreateOrUpdateTuple;
import com.nhl.link.move.writer.TargetPropertyWriter;
import com.nhl.link.move.writer.TargetPropertyWriterFactory;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.map.ObjEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 1.3
 */
public class CreateOrUpdateMerger {

	private static final Logger LOGGER = LoggerFactory.getLogger(CreateOrUpdateMerger.class);

	private ObjEntity objEntity;
	private Mapper mapper;
	private TargetPropertyWriterFactory<?> writerFactory;

	public CreateOrUpdateMerger(ObjEntity objEntity, Mapper mapper, TargetPropertyWriterFactory<?> writerFactory) {
		this.objEntity = objEntity;
		this.mapper = mapper;
		this.writerFactory = writerFactory;
	}

	public void merge(List<CreateOrUpdateTuple<DataObject>> mapped) {
		for (CreateOrUpdateTuple<DataObject> t : mapped) {
			if (!t.isCreated()) {
				merge(t.getSource(), t.getTarget());
			}
		}
	}

	public List<CreateOrUpdateTuple<DataObject>> map(ObjectContext context,
												  Map<Object, Map<String, Object>> mappedSources,
												  List<DataRow> matchedTargets) {

        // clone mappedSources as we are planning to truncate it in this method
		Map<Object, Map<String, Object>> localMappedSources = new HashMap<>(mappedSources);

		List<CreateOrUpdateTuple<DataObject>> result = new ArrayList<>();

		for (DataRow row : matchedTargets) {
			// temporary object
			DataObject object = ((DataContext) context).objectFromDataRow(objEntity.getName(), row);
			Object key = mapper.keyForTarget(object);

			Map<String, Object> src = localMappedSources.remove(key);

			// a null can only mean some algorithm malfunction, as keys are all
			// coming from a known set of sources
			if (src == null) {
				throw new LmRuntimeException("Invalid key: " + key);
			}

			// skip phantom updates...
			if (willUpdate(src, object)) {
				result.add(new CreateOrUpdateTuple<>(src, object, false));
			}
		}

		// everything that's left are new objects
		for (Map.Entry<Object, Map<String, Object>> e : localMappedSources.entrySet()) {

			DataObject t = create((DataContext) context, e.getValue(), true);

			result.add(new CreateOrUpdateTuple<>(e.getValue(), t, true));
		}

		return result;
    }

    protected boolean willUpdate(Map<String, Object> source, DataObject target) {

		if (source.isEmpty()) {
			return false;
		}

		for (Map.Entry<String, Object> e : source.entrySet()) {
			TargetPropertyWriter writer = writerFactory.getOrCreateWriter(e.getKey());
			if (writer == null) {
				LOGGER.info("Source contains property not mapped in the target: " + e.getKey() + ". Skipping...");
				continue;
			}

			if (writer.willWrite(target, e.getValue())) {
                return true;
            }
		}

		return false;
	}

    protected DataObject create(DataContext context, Map<String, Object> source, boolean shouldRegister) {
//		Map<String, Object> target = new HashMap<>();
//		for (Map.Entry<String, Object> e : source.entrySet()) {
//			if (e.getValue() != null) {
//				target.put(e.getKey()/*.substring(3)*/, e.getValue());
//			}
//		}
		DataObject target = (DataObject) context.newObject(objEntity.getName());
		// TODO: ID
//		object.setObjectId(new ObjectId("etl11t_temp", "id", source.get("id")));
		if (shouldRegister) {
			context.registerNewObject(target);
		}
//		target.forEach((k, v) -> {
//			if (!k.equals("id")) {
//				object.writePropertyDirectly(k, v);
//			}
//		});

		for (Map.Entry<String, Object> e : source.entrySet()) {
			TargetPropertyWriter writer = writerFactory.getOrCreateWriter(e.getKey());
			if (writer == null) {
				LOGGER.info("Source contains property not mapped in the target: " + e.getKey() + ". Skipping...");
				continue;
			}
			if (writer.willWrite(target, e.getValue())) {
				writer.write(target, e.getValue());
			}
		}

		return target;
	}

	private void merge(Map<String, Object> source, DataObject target) {

		if (source.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> e : source.entrySet()) {
			TargetPropertyWriter writer = writerFactory.getOrCreateWriter(e.getKey());
			if (writer == null) {
				LOGGER.info("Source contains property not mapped in the target: " + e.getKey() + ". Skipping...");
				continue;
			}
			writer.write(target, e.getValue());
		}
	}
}
