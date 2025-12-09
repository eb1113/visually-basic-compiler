package edu.westminsteru.cmpt355.level3.lang;

import java.util.Map;

public final class RecordType implements ScalarType {
    private final String name;
    private final Map<String, Type> fields; // Useful for descriptor/layout, but not part of equality

    public RecordType(String name, Map<String, Type> fields) {
        this.name = name;
        this.fields = Map.copyOf(fields);
    }

    public String name() {
        return name;
    }

    public Map<String, Type> fields() {
        return fields;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecordType other)) return false;
        // Equality depends only on the record name, not on fields
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "RecordType[" + name + "]";
    }
}
