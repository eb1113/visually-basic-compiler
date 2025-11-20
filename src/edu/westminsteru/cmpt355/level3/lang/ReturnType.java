package edu.westminsteru.cmpt355.level3.lang;

public sealed interface ReturnType
    permits Type, ReturnType.Void {

    public static final ReturnType.Void VOID = Void.INSTANCE;

    enum Void implements ReturnType { INSTANCE }
}
