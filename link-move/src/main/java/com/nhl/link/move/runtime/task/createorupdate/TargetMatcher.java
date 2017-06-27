package com.nhl.link.move.runtime.task.createorupdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.ObjectSelect;

import com.nhl.link.move.mapper.Mapper;

/**
 * @since 1.3
 */
public class TargetMatcher<T> {

	private Supplier<ObjectSelect<T>> queryFactory;
	private Mapper mapper;

	public static TargetMatcher<DataRow> matcher(DbEntity entity, Mapper mapper) {
		return new TargetMatcher<>(DataRow.class, () -> dbQuery(entity), mapper);
	}

	public static <T> TargetMatcher<T> matcher(Class<T> type, Mapper mapper) {
		return new TargetMatcher<>(type, () -> objectQuery(type), mapper);
	}

	private static ObjectSelect<DataRow> dbQuery(DbEntity entity) {
		return ObjectSelect.dbQuery(entity.getName());
	}

	private static <T> ObjectSelect<T> objectQuery(Class<T> type) {
		return ObjectSelect.query(type);
	}

	private TargetMatcher(Class<T> type, Supplier<ObjectSelect<T>> queryFactory, Mapper mapper) {
		this.queryFactory = queryFactory;
		this.mapper = mapper;
	}

	public List<T> match(ObjectContext context, Map<Object, Map<String, Object>> mappedSegment) {

		Collection<Object> keys = mappedSegment.keySet();

		List<Expression> expressions = new ArrayList<>(keys.size());
		for (Object key : keys) {

			Expression e = mapper.expressionForKey(key);
			if (e != null) {
				expressions.add(e);
			}
		}

		// no keys (?)
		if (expressions.isEmpty()) {
			return Collections.emptyList();
		} else {
			return queryFactory.get().where(ExpressionFactory.or(expressions)).select(context);
		}
	}
}
