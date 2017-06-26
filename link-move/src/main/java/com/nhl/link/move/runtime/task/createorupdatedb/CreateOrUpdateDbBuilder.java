package com.nhl.link.move.runtime.task.createorupdatedb;

import com.nhl.link.move.LmRuntimeException;
import com.nhl.link.move.LmTask;
import com.nhl.link.move.annotation.AfterSourceRowsConverted;
import com.nhl.link.move.annotation.AfterSourcesMapped;
import com.nhl.link.move.annotation.AfterTargetsCommitted;
import com.nhl.link.move.annotation.AfterTargetsMatched;
import com.nhl.link.move.annotation.AfterTargetsMerged;
import com.nhl.link.move.extractor.model.ExtractorModel;
import com.nhl.link.move.extractor.model.ExtractorName;
import com.nhl.link.move.mapper.Mapper;
import com.nhl.link.move.runtime.cayenne.ITargetCayenneService;
import com.nhl.link.move.runtime.extractor.IExtractorService;
import com.nhl.link.move.runtime.key.IKeyAdapterFactory;
import com.nhl.link.move.runtime.path.EntityPathNormalizer;
import com.nhl.link.move.runtime.path.IPathNormalizer;
import com.nhl.link.move.runtime.task.BaseTaskBuilder;
import com.nhl.link.move.runtime.task.ListenersBuilder;
import com.nhl.link.move.runtime.task.MapperBuilder;
import com.nhl.link.move.runtime.task.createorupdate.CreateOrUpdateStatsListener;
import com.nhl.link.move.runtime.task.createorupdate.RowConverter;
import com.nhl.link.move.runtime.task.createorupdate.SourceMapper;
import com.nhl.link.move.runtime.token.ITokenManager;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

import java.util.Objects;

/**
 * A builder of an ETL task that matches source data with target data based on a
 * certain unique attribute on both sides.
 */
public class CreateOrUpdateDbBuilder extends BaseTaskBuilder {

	private IExtractorService extractorService;
	private ITargetCayenneService targetCayenneService;
	private ITokenManager tokenManager;
	private MapperBuilder mapperBuilder;
	private Mapper mapper;
	private ListenersBuilder stageListenersBuilder;
	private DbEntity entity;
	private EntityPathNormalizer entityPathNormalizer;

	private ExtractorName extractorName;

	public CreateOrUpdateDbBuilder(String dbEntityName,
								   ITargetCayenneService targetCayenneService,
								   IExtractorService extractorService,
								   ITokenManager tokenManager,
								   IPathNormalizer pathNormalizer,
								   IKeyAdapterFactory keyAdapterFactory) {

		this.targetCayenneService = targetCayenneService;
		this.extractorService = extractorService;
		this.tokenManager = tokenManager;

		Objects.requireNonNull(dbEntityName);
		DbEntity entity = targetCayenneService.entityResolver().getDbEntity(dbEntityName);
		if (entity == null) {
			throw new LmRuntimeException("DbEntity '" + dbEntityName + "' is not mapped in Cayenne");
		}
		this.entity = entity;

		ObjEntity objEntity = new ObjEntity(entity.getName() + "_temp");
		entity.getAttributes().forEach(a -> {
			if (!a.getName().equals("id")) {
				ObjAttribute objAttribute = new ObjAttribute(a.getName(), TypesMapping.getJavaBySqlType(a.getType()), objEntity);
				objAttribute.setDbAttributePath(a.getName());
				objEntity.addAttribute(objAttribute);
			}
		});
//		entity.getRelationships()
		objEntity.setDbEntity(entity);
		targetCayenneService.entityResolver().getDataMap("datamap-targets").addObjEntity(objEntity);

		this.entityPathNormalizer = pathNormalizer.normalizer(entity);
		this.mapperBuilder = new MapperBuilder(entity, entityPathNormalizer, keyAdapterFactory);

		this.stageListenersBuilder = new ListenersBuilder(AfterSourceRowsConverted.class, AfterSourcesMapped.class,
				AfterTargetsMatched.class, AfterTargetsMerged.class, AfterTargetsCommitted.class);

		// always add stats listener..
		stageListener(CreateOrUpdateStatsListener.instance());
	}

	public CreateOrUpdateDbBuilder sourceExtractor(String location, String name) {
		this.extractorName = ExtractorName.create(location, name);
		return this;
	}

	public CreateOrUpdateDbBuilder sourceExtractor(String location) {
		// v.1 model style config
		return sourceExtractor(location, ExtractorModel.DEFAULT_NAME);
	}

	public CreateOrUpdateDbBuilder matchBy(Mapper mapper) {
		this.mapper = mapper;
		return this;
	}

	public CreateOrUpdateDbBuilder matchBy(String... keyAttributes) {
		this.mapper = null;
		this.mapperBuilder.matchBy(keyAttributes);
		return this;
	}

	public CreateOrUpdateDbBuilder matchById() {
		this.mapper = null;
		this.mapperBuilder.matchById();
		return this;
	}

	public CreateOrUpdateDbBuilder batchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	public CreateOrUpdateDbBuilder stageListener(Object listener) {
		stageListenersBuilder.addListener(listener);
		return this;
	}

	public LmTask task() throws IllegalStateException {

		if (extractorName == null) {
			throw new IllegalStateException("Required 'extractorName' is not set");
		}

		return new CreateOrUpdateDbTask(extractorName, batchSize, targetCayenneService, extractorService, tokenManager,
				createProcessor());
	}

	private CreateOrUpdateSegmentProcessor createProcessor() {

		Mapper mapper = this.mapper != null ? this.mapper : mapperBuilder.build();

		SourceMapper sourceMapper = new SourceMapper(mapper);
		TargetMatcher targetMatcher = new TargetMatcher(entity, mapper);
		CreateOrUpdateMerger merger = new CreateOrUpdateMerger(mapper);
		RowConverter rowConverter = new RowConverter(entityPathNormalizer);

		return new CreateOrUpdateSegmentProcessor(rowConverter, sourceMapper, targetMatcher, merger,
				stageListenersBuilder.getListeners());
	}

}
