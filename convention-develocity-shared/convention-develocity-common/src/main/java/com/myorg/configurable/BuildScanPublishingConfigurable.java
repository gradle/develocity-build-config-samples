package com.myorg.configurable;

import java.util.function.Predicate;

public interface BuildScanPublishingConfigurable {

    void onlyIf(Predicate<PublishingContext> onlyIf);

}
