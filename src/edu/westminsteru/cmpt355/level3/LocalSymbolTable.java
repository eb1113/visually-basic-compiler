package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.lang.Function;
import edu.westminsteru.cmpt355.level3.lang.LocalVariable;
import edu.westminsteru.cmpt355.level3.lang.Type;
import edu.westminsteru.cmpt355.level3.lang.Variable;

import java.util.Optional;

/**
 * A local symbol table. At the local level, functions cannot be defined; when resolving, any symbols
 * not found in the local table are resolved in the global table.
 */
public class LocalSymbolTable implements SymbolTable {

    private final GlobalSymbolTable globalTable;

    // suggestion: use a Map<String, LocalVariable> to store symbols

    /** Creates a new local symbol table, descending from a given global symbol table */
    public LocalSymbolTable(GlobalSymbolTable globalTable) {
        this.globalTable = globalTable;
    }

    /**
     * Returns the variable with the given name (checking in the local table first and then the global
     * if not found) or an empty Optional if none is defined.
     * @param name the name of the variable to resolve
     */
    @Override
    public Optional<? extends Variable> resolveVariable(String name) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Returns the function with the given name or an empty Optional if none is defined. (Since functions
     * cannot be defined at the local level, this method just defers to the global table.)
     * @param name the name of the variable to resolve
     */
    @Override
    public Optional<Function> resolveFunction(String name) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Defines a new (local) variable having the given type and name.
     * @return the newly-defined variable
     * @throws DuplicateDefinitionException if there is a conflicting variable with this name
     */
    @Override
    public LocalVariable defineVariable(Type type, String name) throws DuplicateDefinitionException {
        return null;
    }
}
