package net.drehtuer.nextgallery.buildlogic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureDependencyRuleTest {
    @Test
    fun isForbidden_featureToOtherFeature_returnsTrue() {
        assertTrue(FeatureDependencyRule.isForbidden(":feature:folders", ":feature:viewer"))
    }

    @Test
    fun isForbidden_featureToCore_returnsFalse() {
        assertFalse(FeatureDependencyRule.isForbidden(":feature:folders", ":core:ui"))
    }

    @Test
    fun isForbidden_appToFeature_returnsFalse() {
        assertFalse(FeatureDependencyRule.isForbidden(":app", ":feature:folders"))
    }

    @Test
    fun isForbidden_featureToItself_returnsFalse() {
        assertFalse(FeatureDependencyRule.isForbidden(":feature:folders", ":feature:folders"))
    }
}
