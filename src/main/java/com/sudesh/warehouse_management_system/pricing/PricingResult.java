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
public class PricingResult {
    private BigDecimal originalTotal;     // unitPrice * quantity, before discount
    private BigDecimal discountPercentage; // e.g. 10 for 10%
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;        // what actually gets charged
}