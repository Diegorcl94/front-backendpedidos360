package cl.duoc.rabbitadmin.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("admin.security")
public record AdminSecurityProperties(
        @NotBlank @Size(min = 32, max = 512) @Pattern(regexp = "\\S+") String apiKey) {
    @Override
    public String toString() {
        return "AdminSecurityProperties[apiKey=REDACTED]";
    }
}
