package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/** La aplicación arranca contra una base limpia y Flyway aplica todas las migraciones. */
class DemoApplicationIT extends IntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    @Test
    void arranque_baseLimpia_flywayAplicaTodasLasMigraciones() throws Exception {
        try (Connection conexion = dataSource.getConnection();
             Statement consulta = conexion.createStatement();
             ResultSet resultado = consulta.executeQuery(
                     "SELECT COUNT(*) FROM flyway_schema_history WHERE success")) {
            resultado.next();
            assertThat(resultado.getInt(1)).isGreaterThanOrEqualTo(6);
        }
    }
}