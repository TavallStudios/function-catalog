package org.tavall.ai.core.catalog;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AIFunctionScanner {
    public List<Class<?>> scanPackages(Iterable<String> packageNames) {
        Iterable<String> safePackageNames = requireValue(packageNames, "packageNames");
        Set<String> acceptedPackages = new LinkedHashSet<>();
        for (String packageName : safePackageNames) {
            String normalized = normalizePackageName(packageName);
            if (!normalized.isBlank()) {
                acceptedPackages.add(normalized);
            }
        }

        if (acceptedPackages.isEmpty()) {
            return List.of();
        }

        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(acceptedPackages.toArray(String[]::new))
                .scan()) {
            List<Class<?>> result = new ArrayList<>();
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                if (!classInfo.isStandardClass()) {
                    continue;
                }

                result.add(classInfo.loadClass());
            }

            result.sort((left, right) -> left.getName().compareTo(right.getName()));
            return result;
        }
    }

    private static String normalizePackageName(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private static <T> T requireValue(T value, String fieldName) {
        if (value != null) {
            return value;
        }

        throw new IllegalArgumentException(fieldName + " must not be null");
    }
}
