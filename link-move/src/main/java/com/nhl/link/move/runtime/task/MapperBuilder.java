package com.nhl.link.move.runtime.task;

import com.nhl.link.move.mapper.KeyAdapter;
import com.nhl.link.move.mapper.Mapper;
import com.nhl.link.move.mapper.MultiPathMapper;
import com.nhl.link.move.mapper.PathMapper;
import com.nhl.link.move.mapper.SafeMapKeyMapper;
import com.nhl.link.move.runtime.key.IKeyAdapterFactory;
import com.nhl.link.move.runtime.path.EntityPathNormalizer;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @since 1.3
 */
public class MapperBuilder {

	private IKeyAdapterFactory keyAdapterFactory;
	private EntityPathNormalizer pathNormalizer;

	private Optional<ObjEntity> objEntity;
	private DbEntity dbEntity;
	private Set<String> paths;

	public MapperBuilder(ObjEntity entity, EntityPathNormalizer pathNormalizer, IKeyAdapterFactory keyAdapterFactory) {
		this(entity, entity.getDbEntity(), pathNormalizer, keyAdapterFactory);
	}

	public MapperBuilder(DbEntity entity, EntityPathNormalizer pathNormalizer, IKeyAdapterFactory keyAdapterFactory) {
		this(null, entity, pathNormalizer, keyAdapterFactory);
	}

	private MapperBuilder(ObjEntity objEntity, DbEntity dbEntity, EntityPathNormalizer pathNormalizer, IKeyAdapterFactory keyAdapterFactory) {
		this.objEntity = Optional.ofNullable(objEntity);
		this.dbEntity = Objects.requireNonNull(dbEntity);

		this.keyAdapterFactory = keyAdapterFactory;
		this.pathNormalizer = pathNormalizer;

		// Set will weed out simple duplicates , however we don't check for
		// invariants... so duplication is possible via db: vs obj: expressions
		this.paths = new HashSet<>();
	}

	public MapperBuilder matchBy(String... paths) {

		if (paths == null) {
			throw new NullPointerException("Null 'paths'");
		}

		for (String p : paths) {
			this.paths.add(p);
		}

		return this;
	}

	public MapperBuilder matchBy(Property<?>... paths) {

		if (paths == null) {
			throw new NullPointerException("Null 'paths'");
		}

		for (Property<?> p : paths) {
			this.paths.add(p.getName());
		}

		return this;
	}

	public MapperBuilder matchById() {

		Collection<DbAttribute> pks = dbEntity.getPrimaryKeys();
		if (pks.isEmpty()) {
			throw new IllegalStateException("Target entity has no PKs defined: " + dbEntity.getName());
		}

		for (DbAttribute pk : pks) {
			this.paths.add(ASTDbPath.DB_PREFIX + pk.getName());
		}

		return this;
	}

	public Mapper build() {
		return createSafeKeyMapper(createMapper());
	}

	@SuppressWarnings("deprecation")
	Mapper createSafeKeyMapper(Mapper unsafe) {
		if (!objEntity.isPresent()) {
			return unsafe;
		}

		KeyAdapter keyAdapter;

		if (paths.size() > 1) {
			// TODO: mapping keyMapAdapters by type doesn't take into account
			// composition and hierarchy of the keys ... need a different
			// approach. for now resorting to the hacks below

			keyAdapter = keyAdapterFactory.adapter(List.class);
		} else {

			Object attributeOrRelationship = ExpressionFactory.exp(paths.iterator().next()).evaluate(objEntity.get());

			Class<?> type;

			if (attributeOrRelationship instanceof ObjAttribute) {
				type = ((ObjAttribute) attributeOrRelationship).getJavaClass();
			} else if (attributeOrRelationship instanceof ObjRelationship) {
				type = ((ObjRelationship) attributeOrRelationship).getTargetEntity().getJavaClass();
			} else {
				type = null;
			}

			keyAdapter = keyAdapterFactory.adapter(type);
		}

		return new SafeMapKeyMapper(unsafe, keyAdapter);
	}

	Mapper createMapper() {
		Map<String, Mapper> mappers = createPathMappers();
		return mappers.size() > 1 ? new MultiPathMapper(mappers) : mappers.values().iterator().next();
	}

	Map<String, Mapper> createPathMappers() {

		if (paths.isEmpty()) {
			matchById();
		}

		// ensuring predictable attribute iteration order by alphabetically
		// ordering paths and using LinkedHashMap. Useful for unit test for one
		// thing.
		List<String> orderedPaths = new ArrayList<>(paths);
		Collections.sort(orderedPaths);

		Map<String, Mapper> mappers = new LinkedHashMap<>();
		for (String a : orderedPaths) {
			String na = pathNormalizer.normalize(a);
			mappers.put(na, new PathMapper(na));
		}

		return mappers;
	}
}
