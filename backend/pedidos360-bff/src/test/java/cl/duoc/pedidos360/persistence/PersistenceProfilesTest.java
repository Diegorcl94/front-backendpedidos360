package cl.duoc.pedidos360.persistence;

import com.zaxxer.hikari.HikariConfig;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import static org.assertj.core.api.Assertions.assertThat;

class PersistenceProfilesTest {
    @Test
    void perfilPredeterminadoConservaH2SinActivarMysql() throws Exception {
        Properties defaults = load("application.properties");
        assertThat(defaults.getProperty("spring.datasource.url")).startsWith("jdbc:h2:mem:");
        assertThat(defaults.getProperty("spring.datasource.driverClassName")).isEqualTo("org.h2.Driver");
        assertThat(defaults.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("create-drop");
        assertThat(defaults).doesNotContainKeys("spring.profiles.active", "spring.profiles.default", "spring.profiles.include");
    }

    @Test
    void configuracionMysqlExigeIdentidadCaYCredencialesExternasSinAbrirConexion() throws Exception {
        Properties mysql = load("application-mysql.properties");
        assertThat(mysql.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(mysql.getProperty("spring.datasource.driver-class-name")).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(mysql.getProperty("spring.h2.console.enabled")).isEqualTo("false");
        assertThat(mysql).doesNotContainKeys("spring.profiles.active", "spring.profiles.include");
        assertThat(mysql.getProperty("spring.datasource.url"))
                .contains("${MYSQL_HOST}", "${MYSQL_PORT}", "${MYSQL_DATABASE}", "sslMode=VERIFY_IDENTITY")
                .doesNotContain("trustServerCertificate", "useSSL=false", "createDatabaseIfNotExist");
        assertThat(mysql.getProperty("spring.datasource.username")).isEqualTo("${MYSQL_USERNAME}");
        assertThat(mysql.getProperty("spring.datasource.password")).isEqualTo("${MYSQL_PASSWORD}");

        // Solo se enlazan propiedades con valores ficticios; no se activa el perfil ni se crea un DataSource.
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("dummy-local-values", Map.of(
                "MYSQL_HOST", "mysql.example.invalid", "MYSQL_PORT", "12345", "MYSQL_DATABASE", "pedidos360",
                "MYSQL_USERNAME", "test-user", "MYSQL_PASSWORD", "dummy-not-a-real-password",
                "MYSQL_TRUSTSTORE_URL", "file:/C:/private/test-ca.p12",
                "MYSQL_TRUSTSTORE_PASSWORD", "dummy-not-a-real-truststore-password")));
        environment.getPropertySources().addLast(new PropertiesPropertySource("mysql-file-only", mysql));
        environment.getPropertySources().addLast(new PropertiesPropertySource("common-file", load("application.properties")));
        assertThat(environment.getRequiredProperty("spring.datasource.url"))
                .startsWith("jdbc:mysql://mysql.example.invalid:12345/pedidos360?sslMode=VERIFY_IDENTITY");
        DataSourceProperties datasource = Binder.get(environment)
                .bind("spring.datasource", Bindable.of(DataSourceProperties.class)).get();
        // Comprueba que el driver del perfil prevalece sobre driverClassName de H2 en el archivo común.
        assertThat(datasource.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(datasource.getUsername()).isEqualTo("test-user");
        assertThat(datasource.getPassword()).isEqualTo("dummy-not-a-real-password");
        HikariConfig hikari = Binder.get(environment).bind("spring.datasource.hikari", Bindable.of(HikariConfig.class)).get();
        assertThat(hikari.getDataSourceProperties())
                .containsEntry("trustCertificateKeyStoreUrl", "file:/C:/private/test-ca.p12")
                .containsEntry("trustCertificateKeyStoreType", "PKCS12")
                .containsEntry("trustCertificateKeyStorePassword", "dummy-not-a-real-truststore-password")
                .containsEntry("fallbackToSystemTrustStore", "false");
        assertThat(environment.getActiveProfiles()).doesNotContain("mysql");
    }

    private Properties load(String path) throws Exception {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource(path));
    }
}
