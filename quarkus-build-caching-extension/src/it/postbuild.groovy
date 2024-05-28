// Utility functions
String getContent(String fileName) {
    File f = new File(basedir, fileName)
    assert f.exists()
    return f.text
}

void assertBuildCacheDisabled(String logFile) {
    println("Verifying build cache disabled on ${logFile}...")
    String log = getContent(logFile)
    assert log.contains('[quarkus-build-caching-extension] Quarkus build goal marked as not cacheable')
    assert log.contains('Quarkus augmentation completed')
}

void assertBuildCacheMiss(String logFile) {
    println("Verifying build cache miss on ${logFile}...")
    String log = getContent(logFile)
    assert log.contains('[quarkus-build-caching-extension] Quarkus build goal marked as cacheable')
    assert log.contains('Quarkus augmentation completed')

}

void assertBuildCacheHit(String logFile) {
    println("Verifying build cache hit on ${logFile}...")
    String log = getContent(logFile)
    assert log.contains('[quarkus-build-caching-extension] Quarkus build goal marked as cacheable')
    assert !log.contains('Quarkus augmentation completed')
}

// Assertions
assertBuildCacheDisabled('01-uber-jar-build-cache-disabled.log')
assertBuildCacheMiss('02-uber-jar-build-cache-miss.log')
assertBuildCacheHit('03-uber-jar-build-cache-hit.log')
assertBuildCacheDisabled('04-native-build-cache-disabled.log')
assertBuildCacheMiss('05-native-build-cache-miss.log')
assertBuildCacheHit('06-native-build-cache-hit.log')

println('Verification succeeded')
