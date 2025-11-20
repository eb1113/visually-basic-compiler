package edu.westminsteru.cmpt355.level3.lang;

/** Enum of all the data types in the language */
public enum Type implements ReturnType {

    INT, DOUBLE, STRING, BOOLEAN, VOID;

    // Returns the JVM-style type descriptor for this type
    public String getDescriptor() {
        return switch (this) {
            case INT -> "I";
            case DOUBLE -> "D";
            case STRING -> "Ljava/lang/String;";
            case BOOLEAN -> "Z";
            case VOID -> "V";
        };
    }

    public static Type fromString(String s) {
        return switch (s) {
            case "int", "INT" -> Type.INT;
            case "double", "DOUBLE" -> Type.DOUBLE;
            case "string", "STRING" -> Type.STRING;
            case "boolean", "BOOLEAN" -> Type.BOOLEAN;
            case "void", "VOID" -> Type.VOID;
            default -> throw new IllegalArgumentException("Unknown type: " + s);
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
}
