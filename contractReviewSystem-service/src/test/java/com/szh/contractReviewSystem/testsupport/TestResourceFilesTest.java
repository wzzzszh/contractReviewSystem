package com.szh.contractReviewSystem.testsupport;

import java.io.File;

public class TestResourceFilesTest {

    public static void main(String[] args) {
        System.out.println("===== Test TestResourceFiles =====");
        System.out.println();

        verifyExisting("application-test.yml");
        verifyExisting("review/contract/knowledge/risk_knowledge.txt");
        verifyMissing("missing/example.json");

        System.out.println("===== Test Finished =====");
    }

    private static void verifyExisting(String resourcePath) {
        try {
            File file = TestResourceFiles.require(resourcePath);
            System.out.println("[PASS] Found resource: " + resourcePath);
            System.out.println("  Absolute path: " + file.getAbsolutePath());
            System.out.println("  Exists on disk: " + file.exists());
            System.out.println();
        } catch (IllegalStateException e) {
            System.out.println("[FAIL] Expected resource but not found: " + resourcePath);
            System.out.println("  Error: " + e.getMessage());
            System.out.println();
        }
    }

    private static void verifyMissing(String resourcePath) {
        try {
            File file = TestResourceFiles.require(resourcePath);
            System.out.println("[FAIL] Missing resource unexpectedly resolved: " + resourcePath);
            System.out.println("  Absolute path: " + file.getAbsolutePath());
            System.out.println();
        } catch (IllegalStateException e) {
            System.out.println("[PASS] Missing resource rejected as expected: " + resourcePath);
            System.out.println("  Error: " + e.getMessage());
            System.out.println();
        }
    }
}
