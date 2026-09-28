package com.shivhub.backend.dto;

import com.shivhub.backend.enums.VariantReturnDisposition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Does not accept a variant ID: it is always resolved from the original sale item. */
@Data
public class VariantReturnDispositionRequest {
    @NotNull private VariantReturnDisposition disposition;
    @NotNull @Min(1) private Integer quantity;
    @Size(max = 5000) private String remarks;
}
