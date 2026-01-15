package com.myorg;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.myorg.Utils.OwnerAndRepository;
import com.myorg.configurable.BuildScanConfigurable;
import com.myorg.configurable.ExecutionContext;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.myorg.Utils.makeHttpRequest;
import static com.myorg.Utils.readOwnerAndRepository;
import static com.myorg.Utils.readStackTrace;

final class CaptureGitHubCustomPropertyAction implements Consumer<BuildScanConfigurable> {

    // CHANGE ME: Apply your GitHub URL here
    private static final String GITHUB_API_URL = "https://api.github.com";

    // CHANGE ME: Apply the GitHub custom property name here
    private static final String GITHUB_CUSTOM_PROPERTY_NAME = "EricTestProperty";

    private static final String GITHUB_ACCESS_TOKEN = EmbeddedAccessToken.ACCESS_TOKEN;

    // CHANGE ME: Apply your property cache directory name here
    private static final String PROPERTY_CACHE_DIRECTORY_NAME = ".my-company";

    private static final String PROPERTY_CACHE_PATH = String.format("%s/%s.txt", PROPERTY_CACHE_DIRECTORY_NAME, GITHUB_CUSTOM_PROPERTY_NAME);

    private static final TypeToken<List<CustomProperty>> RESPONSE_TYPE = new TypeToken<List<CustomProperty>>() {};

    @Nullable
    private final Path projectDirectory;

    @Nullable
    private final Path writableDirectory;

    @Nullable
    private PropertyCache propertyCache;

    CaptureGitHubCustomPropertyAction(ExecutionContext context) {
        this.projectDirectory = context.getProjectDirectory().orElse(null);
        this.writableDirectory = context.getWritableDirectory().orElse(null);
    }

    @Override
    public void accept(BuildScanConfigurable buildScan) {
        try {
            if (writableDirectory == null) {
                captureWritableDirectoryNotFoundWarning(buildScan);
            } else {
                Optional<String> cachedProperty = loadCachedProperty(writableDirectory);
                if (cachedProperty.isPresent()) {
                    captureProperty(buildScan, cachedProperty.get());
                    return;
                }
            }

            if (projectDirectory == null) {
                captureError(buildScan, "Project directory not found.");
                return;
            }

            Optional<OwnerAndRepository> ownerAndRepository = readOwnerAndRepository(projectDirectory);
            if (!ownerAndRepository.isPresent()) {
                captureError(buildScan, "Owner and repository could not be read.");
                return;
            }

            Optional<CustomProperty> property = fetchCustomProperty(ownerAndRepository.get());
            if (!property.isPresent()) {
                captureError(buildScan, "Custom property '" + GITHUB_CUSTOM_PROPERTY_NAME + "' does not exist for repository.");
                return;
            }

            Object propertyValue = property.get().value;
            if (propertyValue == null) {
                captureEmptyPropertyError(buildScan);
                return;
            } else if (!(propertyValue instanceof String)) {
                captureError(buildScan, "Custom property '" + GITHUB_CUSTOM_PROPERTY_NAME + "' is not a string.");
                return;
            }

            String propertyString = ((String) propertyValue).trim();
            if (propertyString.isEmpty()) {
                captureEmptyPropertyError(buildScan);
                return;
            }

            if (propertyCache != null) {
                propertyCache.writeProperty(propertyString);
            }

            captureProperty(buildScan, propertyString);
        } catch (Exception e) {
            captureError(buildScan, e);
        }
    }

    private Optional<String> loadCachedProperty(Path path) {
        if (propertyCache == null) {
            propertyCache = new PropertyCache(path.resolve(PROPERTY_CACHE_PATH));
        }

        return propertyCache.loadProperty();
    }

    private static Optional<CustomProperty> fetchCustomProperty(OwnerAndRepository ownerAndRepository) throws IOException {
        String url = String.format("%s/repos/%s/%s/properties/values", GITHUB_API_URL, ownerAndRepository.owner, ownerAndRepository.repository);
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/vnd.github+json");
        headers.put("Authorization", "Bearer " + GITHUB_ACCESS_TOKEN);
        headers.put("X-GitHub-Api-Version", "2022-11-28");
        return findProperty(makeHttpRequest(url, RESPONSE_TYPE, headers));
    }

    private static Optional<CustomProperty> findProperty(List<CustomProperty> properties) {
        return properties.stream().filter(property -> GITHUB_CUSTOM_PROPERTY_NAME.equals(property.name)).findFirst();
    }

    private static void captureProperty(BuildScanConfigurable buildScan, String property) {
        buildScan.tag(property);
        buildScan.value(GITHUB_CUSTOM_PROPERTY_NAME, property);
    }

    private static void captureWritableDirectoryNotFoundWarning(BuildScanConfigurable buildScan) {
        buildScan.value(GITHUB_CUSTOM_PROPERTY_NAME + " warning", "Property not cached because a writable directory was not found.");
    }

    private static void captureError(BuildScanConfigurable buildScan, String message) {
        buildScan.value(GITHUB_CUSTOM_PROPERTY_NAME + " error", "Property not captured because: " + message);
    }

    private static void captureError(BuildScanConfigurable buildScan, Exception e) {
        captureError(buildScan, e.getMessage());
        String stackTrace = readStackTrace(e).orElse("Error reading stack trace.");
        buildScan.value(GITHUB_CUSTOM_PROPERTY_NAME + " stack trace", stackTrace);
    }

    private static void captureEmptyPropertyError(BuildScanConfigurable buildScan) {
        captureError(buildScan, "Custom property '" + GITHUB_CUSTOM_PROPERTY_NAME + "' is empty.");
    }

    private static final class CustomProperty {

        @SerializedName("property_name")
        String name;
        Object value;
    }

}
