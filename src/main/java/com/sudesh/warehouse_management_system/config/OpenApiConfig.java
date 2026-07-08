package com.sudesh.warehouse_management_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI warehouseManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Warehouse Order & Inventory Management System API")
                        .version("1.0")
                        .description("REST API for managing products, warehouses, inventory, customers, and orders for a distribution business.")
                        .contact(new Contact()
                                .name("Sudesh Hansika")
                                .url("https://github.com/Sudesh-2002")));
    }
}