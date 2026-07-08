package com.sudesh.warehouse_management_system.pricing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingContext {
    private BigDecimal unitPrice;
    private Integer quantity;
}