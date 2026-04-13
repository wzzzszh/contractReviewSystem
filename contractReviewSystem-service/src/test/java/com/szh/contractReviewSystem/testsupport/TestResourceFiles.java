package com.szh.contractReviewSystem.testsupport;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestResourceFiles {

    private TestResourceFiles() {
    }

    public static File require(String resourcePath) {
        URL resource = TestResourceFiles.class.getClassLoader().getResource(resourcePath);

        if (resource == null) {
            throw new IllegalStateException(
                    "Test resource not found: " + resourcePath + ", user.dir=" + System.getProperty("user.dir"));
        }
        try {
            Path path = Paths.get(resource.toURI());

            return path.toFile();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve test resource: " + resourcePath, e);
        }
    }
}
