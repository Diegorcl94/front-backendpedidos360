package cl.duoc.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearBindingRequest(
        @NotBlank @Size(max = 127) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String queue,
        @NotBlank @Size(max = 127) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String exchange,
        @NotBlank @Size(max = 255) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String routingKey) {}
