package cl.duoc.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearExchangeRequest(
        @NotBlank @Size(max = 127) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String name) {}
