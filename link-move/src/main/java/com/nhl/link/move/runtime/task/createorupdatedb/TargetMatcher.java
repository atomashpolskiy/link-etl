package com.nhl.link.move.runtime.task.createorupdatedb;

import com.nhl.link.move.mapper.Mapper;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.ObjectSelect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TargetMatcher {

	private DbEntity entity;
	private Mapper mapper;

	public TargetMatcher(DbEntity entity, Mapper mapper) {
		this.entity = entity;
		this.mapper = mapper;
	}

	public List<DataRow> match(ObjectContext context, Map<Object, Map<String, Object>> mappedSegment) {

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
			return ObjectSelect.dbQuery(entity.getFullyQualifiedName()).where(ExpressionFactory.or(expressions)).select(context);
		}
	}
}
