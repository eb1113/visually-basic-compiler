package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.lang.*;

import java.util.List;
import java.util.Optional;

/**
 * A global symbol table. At the global level, not only variables but also
 * functions can be defined.
 */
public class GlobalSymbolTable implements SymbolTable {

    // Suggestion: use a Map<String, GlobalVariable> and Map<String, Function> to
    // store the symbols

    /**
     * Returns the (global) variable with the given name or an empty Optional if none is defined.
     * @param name the name of the variable to resolve
     */
    @Override
    public Optional<GlobalVariable> resolveVariable(String name) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Returns the function with the given name or an empty Optional if none is defined.
     * @param name the name of the function to resolve
     */
    @Override
    public Optional<Function> resolveFunction(String name) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Defines a new (global) variable having the given type and name.
     * @return the newly-defined variable
     * @throws DuplicateDefinitionException if there is a conflicting variable with this name
     */
    @Override
    public GlobalVariable defineVariable(Type type, String name) throws DuplicateDefinitionException {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Defines a new function having the given return type, name, and parameter types.
     * @return the newly-defined function
     * @throws DuplicateDefinitionException if there is a conflicting function with this name
     */
    public Function defineFunction(ReturnType returnType, String name, List<Type> parameterTypes) throws DuplicateDefinitionException{
        throw new RuntimeException("Not implemented");
    }

    /**
     * Returns a list of all (global) variables defined in this symbol table
     */
    public List<GlobalVariable> getGlobalVariables() {
        throw new RuntimeException("Not implemented");
    }
}
