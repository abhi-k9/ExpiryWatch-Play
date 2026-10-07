plugins {
    alias(libs.plugins.expirywatch.android.library)
    alias(libs.plugins.expirywatch.hilt)
}

android {
    namespace = "io.github.abhik9.expirywatch.core.billing"
}

dependencies {
    api(projects.core.donations)
    implementation(libs.billing)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
}
