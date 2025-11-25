package edu.westminsteru.cmpt355.level3.lang;


/**
 * Built-in scalar types: int, double, string, boolean, void.
 */
public enum BuiltInType implements ScalarType {
    INT, DOUBLE, STRING, BOOLEAN, VOID;

    // Returns the JVM-style oldType descriptor for this oldType
    public String getDescriptor() {
        return switch (this) {
            case INT -> "I";
            case DOUBLE -> "D";
            case STRING -> "Ljava/lang/String;";
            case BOOLEAN -> "Z";
            case VOID -> "V";
        };
    }

    public static BuiltInType fromString(String s) {
        return switch (s) {
            case "int", "INT" -> BuiltInType.INT;
            case "double", "DOUBLE" -> BuiltInType.DOUBLE;
            case "string", "STRING" -> BuiltInType.STRING;
            case "boolean", "BOOLEAN" -> BuiltInType.BOOLEAN;
            case "void", "VOID" -> BuiltInType.VOID;
            default -> throw new IllegalArgumentException("Unknown oldType: " + s);
        };
    }

    public int size() {
        return switch (this) {
            case INT -> 1;
            case DOUBLE -> 2;
            case STRING -> 1;
            case BOOLEAN -> 1;
            case VOID -> 0;
        };
    }

    @Override
    public boolean isNumeric() {
        return this == INT || this == DOUBLE;
    }

    @Override
    public boolean isBoolean() {
        return this == BOOLEAN;
    }

    @Override
    public boolean isVoid() {
        return this == VOID;
    }

    @Override
    public boolean isReference() {
        // STRING is a reference type; others are primitives
        return this == STRING;
    }
}
