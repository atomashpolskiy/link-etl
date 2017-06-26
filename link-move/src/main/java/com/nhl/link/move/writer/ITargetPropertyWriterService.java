package com.nhl.link.move.writer;

import org.apache.cayenne.map.ObjEntity;

/**
 * @since 1.6
 */
public interface ITargetPropertyWriterService {

    <T> TargetPropertyWriterFactory<T> getWriterFactory(Class<T> type);

    TargetPropertyWriterFactory<?> getWriterFactory(ObjEntity entity);
}
