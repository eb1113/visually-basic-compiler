package edu.westminsteru.cmpt355.level3.lang;

public sealed interface ReturnType
        permits ReturnType.Void, Type {

    public static final ReturnType.Void VOID = Void.INSTANCE;

    String getDescriptor();

    /**
     * Converts a string to a ReturnType (Type or VOID)
     */
    static ReturnType fromString(String s) {
        if (s.equalsIgnoreCase("void")) {
            return ReturnType.VOID;
        } else {
            return Type.fromBuiltInString(s);
        }
    }

    enum Void implements ReturnType {
        INSTANCE;

        @Override
        public String getDescriptor() {
            return "V";
        }
    }
}
