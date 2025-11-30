package edu.westminsteru.cmpt355.level3;

import edu.westminsteru.cmpt355.level3.lang.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A global symbol table. At the global level, not only variables but also
 * functions can be defined.
 */
public class GlobalSymbolTable implements SymbolTable {

    private final Map<String, GlobalVariable> globalTable = new HashMap<String, GlobalVariable>();
    private final Map<String,Function> functionTable = new HashMap<String, Function>();
    private final Map<String, RecordSymbolTable> recordTable = new HashMap<String, RecordSymbolTable>();

    /**
     * Returns the (global) variable with the given name or an empty Optional if none is defined.
     * @param name the name of the variable to resolve
     */
    @Override
    public Optional<GlobalVariable> resolveVariable(String name) {
        if(globalTable.containsKey(name)){
            return Optional.ofNullable(globalTable.get(name));
        }
        else{
            return Optional.empty();
        }
    }

    /**
     * Returns the function with the given name or an empty Optional if none is defined.
     * @param name the name of the function to resolve
     */
    @Override
    public Optional<Function> resolveFunction(String name) {
        if(functionTable.containsKey(name)){
            return Optional.ofNullable(functionTable.get(name));
        }
        else{
            return Optional.empty();
        }
    }

    /**
     * Defines a new (global) variable having the given type and name.
     * @return the newly-defined variable
     * @throws DuplicateDefinitionException if there is a conflicting variable with this name
     */
    @Override
    public GlobalVariable defineVariable(Type type, String name) throws DuplicateDefinitionException {
        if(globalTable.containsKey(name)){
            throw new DuplicateDefinitionException("Variable " + name + " is already defined in this scope.");
        }
        else{
            GlobalVariable newVar = new GlobalVariable(type, name);
            globalTable.put(name, newVar);
            return newVar;
        }
    }

    /**
     * Defines a new function having the given return type, name, and parameter types.
     * @return the newly-defined function
     * @throws DuplicateDefinitionException if there is a conflicting function with this name
     */
    public Function defineFunction(ReturnType returnType, String name, List<Type> parameterTypes) throws DuplicateDefinitionException{
        if(functionTable.containsKey(name)){
            throw new DuplicateDefinitionException("Function " + name + " is already defined in this scope.");
        }
        else{
            Function newFunc = new Function(name, returnType, parameterTypes);
            functionTable.put(name, newFunc);
            return newFunc;
        }
    }

    /**
     * Returns a list of all (global) variables defined in this symbol table
     */
    public List<GlobalVariable> getGlobalVariables() {
        return List.copyOf(globalTable.values());
    }

    /**
     * Returns the record symbol table with the given name or an empty Optional if none is defined.
     * @param name the name of the record to resolve
     */
    public Optional<RecordSymbolTable> resolveRecord(String name) {
        if (recordTable.containsKey(name)) {
            return Optional.ofNullable(recordTable.get(name));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Defines a new record having the given name and symbol table.
     * @param name the name of the record
     * @param recordSymbolTable the symbol table containing the record's fields and methods
     * @return the newly-defined record symbol table
     * @throws DuplicateDefinitionException if there is a conflicting record with this name
     */
    public RecordSymbolTable defineRecord(String name, RecordSymbolTable recordSymbolTable) throws DuplicateDefinitionException {
        if (recordTable.containsKey(name)) {
            throw new DuplicateDefinitionException("Record " + name + " is already defined in this scope.");
        } else {
            recordTable.put(name, recordSymbolTable);
            return recordSymbolTable;
        }
    }

    /**
     * Returns all record names defined in this symbol table.
     * @return A set of all record names.
     */
    public java.util.Set<String> getAllRecordNames() {
        return java.util.Set.copyOf(recordTable.keySet());
    }

}
