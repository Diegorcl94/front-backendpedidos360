package cl.duoc.rabbitadmin;

import cl.duoc.rabbitadmin.config.AdminSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

class AdminSecurityPropertiesTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AdminSecurityProperties.class)
    static class SecurityOnly {}

    @Test
    void claveAusenteOCortaImpideArranque() {
        var runner = new ApplicationContextRunner().withUserConfiguration(SecurityOnly.class);
        runner.run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("admin.security.api-key=corta")
                .run(context -> assertThat(context).hasFailed());
    }
}
