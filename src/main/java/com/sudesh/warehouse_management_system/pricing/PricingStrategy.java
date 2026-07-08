package com.sudesh.warehouse_management_system.pricing;

public interface PricingStrategy {

    /**
     * Calculates the final price for a line item given quantity and base unit price.
     */
    PricingResult calculatePrice(PricingContext context);
}