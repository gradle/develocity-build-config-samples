// Utility functions
String getContent(String fileName) {
    File f = new File(basedir, fileName)
    assert f.exists()
    return f.text
}

void checkBuildCacheDisabled(String logFile) {
    println("Verifying build cache disabled on ${logFile}...")
    String log = getContent(logFile)
    assert log.contains( '[quarkus-build-caching-extension] Quarkus build goal marked as not cacheable')
    assert log.contains( 'Quarkus augmentation completed')
}

void checkBuildCacheMiss(String logFile) {
    println("Verifying build cache miss on ${logFile}...")
    String log = getContent(logFile)
    assert log.contains('[quarkus-build-caching-extension] Quarkus build goal marked as cacheable')
    assert log.contains('Quarkus augmentation completed')

}

void checkBuildCacheHit(String logFile) {
    println("Verifying build cache hit on ${logFile}...")
    String log = getContent(logFile)
    assert log.contains( '[quarkus-build-caching-extension] Quarkus build goal marked as cacheable' )
    assert !log.contains( 'Quarkus augmentation completed' )
}

// Assertions
checkBuildCacheDisabled('01-uber-jar-build-cache-disabled.log')
checkBuildCacheMiss('02-uber-jar-build-cache-miss.log')
checkBuildCacheHit('03-uber-jar-build-cache-hit.log')
checkBuildCacheDisabled('04-native-build-cache-disabled.log')
checkBuildCacheMiss('05-native-build-cache-miss.log')
checkBuildCacheHit('06-native-build-cache-hit.log')

println('Verification succeeded')
