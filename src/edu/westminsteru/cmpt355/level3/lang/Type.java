package edu.westminsteru.cmpt355.level3.lang;

public sealed interface Type extends ReturnType
    permits ScalarType, ArrayType {

    /**
     * JVM-style descriptor, e.g. "I", "D", "Ljava/lang/String;", "[I", "Lpkg/Point;"
     */
    String getDescriptor();

    /**
     * JVM "slot" size:
     *  - 1 for int, boolean, reference types
     *  - 2 for double
     *  - 0 for void
     */
    int size();

    default boolean isNumeric() {
        return false;
    }
    default boolean isBoolean() {
        return false;
    }
    default boolean isVoid() {
        return false;
    }

    /**
     * @return true if this type is stored as a reference on JVM stack
     * (e.g. arrays/records; String in BuiltInType but still a reference)
     */
    default boolean isReference() {
        // ArrayType always a reference
        return this instanceof ArrayType;
    }

    /**
     * Converts a BuiltInType string to a Type.
     * For use during parsing when we know it's a built-in type.
     */
    static Type fromBuiltInString(String s) {
        BuiltInType builtIn = BuiltInType.fromString(s);
        return builtIn;
    }
}
