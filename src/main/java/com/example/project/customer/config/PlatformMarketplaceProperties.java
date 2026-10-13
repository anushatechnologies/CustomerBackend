package com.example.project.customer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.marketplace")
public class PlatformMarketplaceProperties {

    /**
     * Store ID of the official HinchStore platform store. Defaults to 1 if unset.
     */
    private Integer platformStoreId = 1;

    /**
     * Feature flag gating HinchStore checkout in Phase 1 pending Phase 2 vendor consignment rollout.
     */
    private boolean hinchStoreCheckoutEnabled = false;

    public boolean isPlatformStore(Integer storeId) {
        return storeId != null && storeId.equals(platformStoreId);
    }
}
