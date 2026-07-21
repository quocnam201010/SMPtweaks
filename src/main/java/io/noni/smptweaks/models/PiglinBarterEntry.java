package io.noni.smptweaks.models;

import java.util.List;

public record PiglinBarterEntry(
        String name,
        List<CoordinateCondition> conditions,
        List<PiglinBarterModifierItem> items
) {}
