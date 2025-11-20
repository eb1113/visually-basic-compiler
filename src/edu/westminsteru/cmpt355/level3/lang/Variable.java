package edu.westminsteru.cmpt355.level3.lang;

/**
 * Sealed interface representing a variable in the language.
 * <br>
 * N.B.: this is not an IR node and hence should not contain IR nodes:
 * instances of this interface should contain just the needed information to
 * access the relevant variable.
 */
public sealed interface Variable
    permits GlobalVariable, LocalVariable {

    String name();
    Type type();
}
