package com.nhl.link.move.runtime.path;

import com.nhl.link.move.LmRuntimeException;
import com.nhl.link.move.runtime.jdbc.JdbcNormalizer;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.PathComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @since 1.4
 */
public class PathNormalizer implements IPathNormalizer {

	private ConcurrentMap<String, EntityPathNormalizer> pathCache;
	private Map<String, JdbcNormalizer> jdbcNormalizers;

	public PathNormalizer(@Inject Map<String, JdbcNormalizer> jdbcNormalizers) {
		pathCache = new ConcurrentHashMap<>();
		this.jdbcNormalizers = jdbcNormalizers;
	}

	@Override
	public EntityPathNormalizer normalizer(ObjEntity root) {

		if (root == null) {
			throw new NullPointerException("Null root entity");
		}

		EntityPathNormalizer normalizer = pathCache.get(root.getName());

		if (normalizer == null) {
			EntityPathNormalizer newNormalizer = createNormalizer(root);
			EntityPathNormalizer oldNormalizer = pathCache.putIfAbsent(root.getName(), newNormalizer);
			normalizer = oldNormalizer != null ? oldNormalizer : newNormalizer;
		}

		return normalizer;
	}

	@Override
	public EntityPathNormalizer normalizer(DbEntity root) {

		if (root == null) {
			throw new NullPointerException("Null root entity");
		}

		String name = ASTDbPath.DB_PREFIX + root.getName();
		EntityPathNormalizer normalizer = pathCache.get(name);

		if (normalizer == null) {
			EntityPathNormalizer newNormalizer = createNormalizer(root);
			EntityPathNormalizer oldNormalizer = pathCache.putIfAbsent(name, newNormalizer);
			normalizer = oldNormalizer != null ? oldNormalizer : newNormalizer;
		}

		return normalizer;
	}

	private EntityPathNormalizer createNormalizer(final ObjEntity entity) {

		return new EntityPathNormalizer() {

			ConcurrentMap<String, AttributeInfo> cachedAttributes = new ConcurrentHashMap<>();

			@Override
			public String normalize(String path) {

				if (!hasAttribute(entity, path)) {
					return path;
				}

				if (path == null) {
					throw new NullPointerException("Null path. Entity: " + entity.getName());
				}

				return getAttributeInfo(path).getNormalizedPath();
			}

			@Override
			public Object normalizeValue(String path, Object value) {

				if (!hasAttribute(entity, path)) {
					return value;
				}

				if (value == null) {
					return null;
				}

				AttributeInfo attributeInfo = getAttributeInfo(path);

				ObjAttribute objAttribute = entity.getAttributeForDbAttribute(attributeInfo.getTarget());
				String javaType = (objAttribute == null) ? TypesMapping.getJavaBySqlType(attributeInfo.getType())
						: toObjectType(objAttribute.getType());

				JdbcNormalizer<?> normalizer = jdbcNormalizers.get(javaType);
				if (normalizer == null) {
					return value;
				} else {
					return normalizer.normalize(value, attributeInfo.getTarget());
				}
			}

			private boolean hasAttribute(ObjEntity entity, String path) {
				if (path.startsWith(ASTDbPath.DB_PREFIX)) {
					path = path.replace(ASTDbPath.DB_PREFIX, "");
					return entity.getDbEntity().getAttributeMap().containsKey(path)
							|| entity.getDbEntity().getRelationshipMap().containsKey(path);
				} else {
					return entity.getAttributeMap().containsKey(path) || entity.getRelationshipMap().containsKey(path);
				}
			}

			private AttributeInfo getAttributeInfo(String path) {

				AttributeInfo normalizedInfo = cachedAttributes.get(path);
				if (normalizedInfo == null) {
					AttributeInfo newNormalizedInfo = doNormalize(path);
					AttributeInfo oldNormalizedInfo = cachedAttributes.putIfAbsent(path, newNormalizedInfo);
					normalizedInfo = oldNormalizedInfo != null ? oldNormalizedInfo : newNormalizedInfo;
				}

				return normalizedInfo;
			}

			private AttributeInfo doNormalize(String path) {

				Expression dbExp = entity.translateToDbPath(ExpressionFactory.exp(path));

				List<PathComponent<DbAttribute, DbRelationship>> components = new ArrayList<>(2);
				for (PathComponent<DbAttribute, DbRelationship> c : entity.getDbEntity().resolvePath(dbExp,
						Collections.emptyMap())) {
					components.add(c);
				}

				if (components.size() == 0) {
					throw new LmRuntimeException("Null path. Entity: " + entity.getName());
				}

				if (components.size() > 1) {
					throw new LmRuntimeException("Nested paths not supported. Path: " + dbExp);
				}

				PathComponent<DbAttribute, DbRelationship> c = components.get(0);

				if (c.getAttribute() != null) {
					return AttributeInfo.forAttribute(c.getAttribute());
				}

				DbRelationship dbRelationship = c.getRelationship();

				List<DbJoin> joins = dbRelationship.getJoins();

				// TODO: logic duplication from DefaultCreateOrUpdateBuilder...
				if (joins.size() > 1) {
					// TODO: support for multi-key to-one relationships
					throw new LmRuntimeException("Multi-column FKs are not yet supported. Path: " + dbExp);
				} else {
					return AttributeInfo.forRelationship(joins.get(0));
				}
			}
		};
	}

	private EntityPathNormalizer createNormalizer(DbEntity entity) {

		return new EntityPathNormalizer() {

			ConcurrentMap<String, AttributeInfo> cachedAttributes = new ConcurrentHashMap<>();

			@Override
			public String normalize(String path) {

				if (!hasAttribute(entity, path)) {
					return path;
				}

				if (path == null) {
					throw new NullPointerException("Null path. Entity: " + entity.getName());
				}

				return getAttributeInfo(path).getNormalizedPath();
			}

			@Override
			public Object normalizeValue(String path, Object value) {

				if (!hasAttribute(entity, path)) {
					return value;
				}

				if (value == null) {
					return null;
				}

				AttributeInfo attributeInfo = getAttributeInfo(path);

				String javaType = TypesMapping.getJavaBySqlType(attributeInfo.getType());

				JdbcNormalizer<?> normalizer = jdbcNormalizers.get(javaType);
				if (normalizer == null) {
					return value;
				} else {
					return normalizer.normalize(value, attributeInfo.getTarget());
				}
			}

			private boolean hasAttribute(DbEntity entity, String path) {
				if (path.startsWith(ASTDbPath.DB_PREFIX)) {
					path = path.replace(ASTDbPath.DB_PREFIX, "");
				}
				return entity.getAttributeMap().containsKey(path) || entity.getRelationshipMap().containsKey(path);
			}

			private AttributeInfo getAttributeInfo(String path) {

				AttributeInfo normalizedInfo = cachedAttributes.get(path);
				if (normalizedInfo == null) {
					AttributeInfo newNormalizedInfo = doNormalize(path);
					AttributeInfo oldNormalizedInfo = cachedAttributes.putIfAbsent(path, newNormalizedInfo);
					normalizedInfo = oldNormalizedInfo != null ? oldNormalizedInfo : newNormalizedInfo;
				}

				return normalizedInfo;
			}

			private AttributeInfo doNormalize(String path) {

				Expression dbExp = path.startsWith(ASTDbPath.DB_PREFIX) ? ExpressionFactory.exp(path) : ExpressionFactory.dbPathExp(path);

				List<PathComponent<DbAttribute, DbRelationship>> components = new ArrayList<>(2);
				for (PathComponent<DbAttribute, DbRelationship> c : entity.resolvePath(dbExp,
						Collections.emptyMap())) {
					components.add(c);
				}

				if (components.size() == 0) {
					throw new LmRuntimeException("Null path. Entity: " + entity.getName());
				}

				if (components.size() > 1) {
					throw new LmRuntimeException("Nested paths not supported. Path: " + dbExp);
				}

				PathComponent<DbAttribute, DbRelationship> c = components.get(0);

				if (c.getAttribute() != null) {
					return AttributeInfo.forAttribute(c.getAttribute());
				}

				DbRelationship dbRelationship = c.getRelationship();

				List<DbJoin> joins = dbRelationship.getJoins();

				// TODO: logic duplication from DefaultCreateOrUpdateBuilder...
				if (joins.size() > 1) {
					// TODO: support for multi-key to-one relationships
					throw new LmRuntimeException("Multi-column FKs are not yet supported. Path: " + dbExp);
				} else {
					return AttributeInfo.forRelationship(joins.get(0));
				}
			}
		};
	}

	private static String toObjectType(String primitiveType) {
		if (primitiveType == null) {
			return null;
		}

		switch (primitiveType) {
		case "byte": {
			return Byte.class.getName();
		}
		case "char": {
			return Character.class.getName();
		}
		case "short": {
			return Short.class.getName();
		}
		case "int": {
			return Integer.class.getName();
		}
		case "long": {
			return Long.class.getName();
		}
		case "float": {
			return Float.class.getName();
		}
		case "double": {
			return Double.class.getName();
		}
		case "boolean": {
			return Boolean.class.getName();
		}
		default: {
			// not a primitive type
			return primitiveType;
		}
		}
	}

	private static abstract class AttributeInfo {

		public abstract String getNormalizedPath();

		public abstract int getType();

		public abstract DbAttribute getTarget();

		public static AttributeInfo forAttribute(final DbAttribute attribute) {
			return new AttributeInfo() {
				@Override
				public String getNormalizedPath() {
					return ASTDbPath.DB_PREFIX + attribute.getName();
				}

				@Override
				public int getType() {
					return attribute.getType();
				}

				@Override
				public DbAttribute getTarget() {
					return attribute;
				}
			};
		}

		public static AttributeInfo forRelationship(final DbJoin join) {
			return new AttributeInfo() {
				@Override
				public String getNormalizedPath() {
					return ASTDbPath.DB_PREFIX + join.getSourceName();
				}

				@Override
				public int getType() {
					return join.getSource().getType();
				}

				@Override
				public DbAttribute getTarget() {
					return join.getTarget();
				}
			};
		}
	}
}
