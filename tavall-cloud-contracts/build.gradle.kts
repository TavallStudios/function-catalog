dependencies {
    api(project(":ai-core"))
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    testRuntimeOnly(libs.junit.launcher)
}
