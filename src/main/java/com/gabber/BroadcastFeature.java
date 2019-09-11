package com.gabber;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

public class BroadcastFeature  implements DynamicFeature {
    private ShouldBroadcastFilter shouldBroadcastFilter;

    public BroadcastFeature(ShouldBroadcastFilter shouldBroadcastFilter) {
        this.shouldBroadcastFilter = shouldBroadcastFilter;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceMethod().getAnnotation(BroadcastIt.class) != null) {
            context.register(shouldBroadcastFilter);
        }
    }

}