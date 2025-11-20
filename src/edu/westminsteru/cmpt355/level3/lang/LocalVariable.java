package edu.westminsteru.cmpt355.level3.lang;

/**
 * Record representing a local variable (or parameter) in the language
 * <br>
 * N.B.: this is not an IR node and hence should not contain IR nodes:
 * instances of this interface should contain just the needed information to
 * access the relevant variable.
 */
public record LocalVariable(Type type, String name, int index) implements Variable {
}
