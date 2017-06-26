package com.nhl.link.move.itest;

import com.nhl.link.move.Execution;
import com.nhl.link.move.LmRuntimeException;
import com.nhl.link.move.LmTask;
import com.nhl.link.move.runtime.task.ITaskService;
import com.nhl.link.move.unit.LmIntegrationTest;
import com.nhl.link.move.unit.cayenne.t.Etl1t;
import com.nhl.link.move.unit.cayenne.t.Etl3t;
import com.nhl.link.move.unit.cayenne.t.Etl5t;
import com.nhl.link.move.unit.cayenne.t.Etl9t;
import org.apache.cayenne.query.EJBQLQuery;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CreateOrUpdateDbIT extends LmIntegrationTest {

	@Test
	public void test_ByAttribute() {

		LmTask task = etl.service(ITaskService.class).createOrUpdate("etl11t")
				.sourceExtractor("com/nhl/link/move/itest/etl11_to_etl11t").matchBy("name").task();

		srcRunSql("INSERT INTO utest.etl11 (ID, NAME, AGE) VALUES (1, 'a', 1)");
		srcRunSql("INSERT INTO utest.etl11 (ID, NAME, AGE) VALUES (2, 'b', 2)");

		Execution e1 = task.run();
		assertExec(2, 2, 0, 0, e1);
		assertEquals(2, targetScalar("SELECT count(1) from utest.etl11t"));
		assertEquals(1, targetScalar("SELECT count(1) from utest.etl11t WHERE NAME = 'a' AND AGE = 1"));
		assertEquals(1, targetScalar("SELECT count(1) from utest.etl11t WHERE NAME = 'b' AND AGE = 2"));

		srcRunSql("INSERT INTO utest.etl11 (ID, NAME) VALUES (3, 'c')");
		srcRunSql("UPDATE utest.etl11 SET AGE = 5 WHERE NAME = 'a'");

		Execution e2 = task.run();
		assertExec(3, 1, 1, 0, e2);
		assertEquals(3, targetScalar("SELECT count(1) from utest.etl11t"));
		assertEquals(1, targetScalar("SELECT count(1) from utest.etl11t WHERE NAME = 'a' AND age = 5"));
		assertEquals(1, targetScalar("SELECT count(1) from utest.etl11t WHERE NAME = 'c' AND age is null"));

		srcRunSql("DELETE FROM utest.etl11 WHERE NAME = 'a'");

		Execution e3 = task.run();
		assertExec(2, 0, 0, 0, e3);
		assertEquals(3, targetScalar("SELECT count(1) from utest.etl11t"));
		assertEquals(1, targetScalar("SELECT count(1) from utest.etl11t WHERE NAME = 'a'"));

		Execution e4 = task.run();
		assertExec(2, 0, 0, 0, e4);
	}
}
