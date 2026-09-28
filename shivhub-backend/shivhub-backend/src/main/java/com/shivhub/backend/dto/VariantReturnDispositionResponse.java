package com.shivhub.backend.dto;

import com.shivhub.backend.enums.VariantReturnDisposition;
import lombok.Data;

@Data
public class VariantReturnDispositionResponse {
    private Long requestId;
    private Long variantId;
    private VariantReturnDisposition disposition;
    private Integer quantity;
    private boolean stockRestored;
    private Integer availableStock;
}
