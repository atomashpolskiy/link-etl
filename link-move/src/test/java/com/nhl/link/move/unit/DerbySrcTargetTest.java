package com.nhl.link.move.unit;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.nhl.link.move.unit.cayenne.t.Etl1t;

public abstract class DerbySrcTargetTest extends DerbySrcTest {

	protected static CayenneDerbyStack targetStack;
	protected ObjectContext targetContext;

	@BeforeClass
	public static void startTarget() throws IOException, SQLException {
		targetStack = new CayenneDerbyStack("derbytarget", "cayenne-linketl-tests-targets.xml");
	}

	@AfterClass
	public static void shutdownTarget() throws IOException, SQLException {
		targetStack.shutdown();
	}

	@Before
	public void prepareTarget() {

		targetContext = targetStack.newContext();

		// first query in a test set will also load the schema...

		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl1t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl3t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl2t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl4t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl5t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl6t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl7t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl8t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl9t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl10t"));
		targetContext.performGenericQuery(new SQLTemplate(Etl1t.class, "DELETE from utest.etl11t"));
	}

	protected void targetRunSql(String sql) {
		targetContext.performGenericQuery(new SQLTemplate(Object.class, sql));
	}

	protected int targetScalar(String sql) {
		SQLSelect<Integer> query = SQLSelect.scalarQuery(Integer.class, sql);
		return query.selectOne(targetContext).intValue();
	}

}
