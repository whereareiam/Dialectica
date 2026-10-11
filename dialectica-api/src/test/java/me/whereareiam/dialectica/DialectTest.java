package me.whereareiam.dialectica;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialectTest {
	private static DatabaseMetaData product(String name) {
		return (DatabaseMetaData) Proxy.newProxyInstance(DialectTest.class.getClassLoader(), new Class<?>[]{DatabaseMetaData.class},
				(proxy, method, arguments) -> {
					if (method.getName().equals("getDatabaseProductName")) return name;

					throw new UnsupportedOperationException(method.getName());
				});
	}

	@Test
	void discoversADialectRegisteredOnTheClasspath() {
		assertTrue(Dialect.available().stream().anyMatch(dialect -> dialect.name().equals("ThirdParty")));
	}

	@Test
	void picksTheDialectThatAcceptsTheDatabase() throws SQLException {
		Dialect dialect = Dialect.of(product("ThirdParty"));

		assertInstanceOf(ThirdPartyDialect.class, dialect);
		assertEquals("ThirdParty", dialect.name());
	}

	@Test
	void refusesADatabaseNoDialectAccepts() {
		assertThrows(IllegalArgumentException.class, () -> Dialect.of(product("Oracle")));
	}

	@Test
	void refusesADatabaseSeveralDialectsAccept() {
		assertThrows(IllegalStateException.class, () -> Dialect.of(product("Contested")));
	}
}
