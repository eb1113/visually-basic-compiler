package edu.westminsteru.cmpt355.level3.lang;

import java.util.Map;

public record RecordType(String name, Map<String, Type> fields) implements ScalarType {
    // Record name e.g. Point
    // Field types e.g. int, array, etc.
    @Override
    public String getDescriptor() {
        // JVM class descriptor format: L<package>/<name>;
        // Convert record name to JVM format (replace dots with slashes)
        String jvmName = name.replace('.', '/');
        return "L" + jvmName + ";";
    }

    @Override
    public int size() {
        // Records are reference types on JVM
        return 1;
    }

    @Override
    public boolean isReference() {
        return true;
    }
}
