package edu.westminsteru.cmpt355.level3.ir;

import edu.westminsteru.cmpt355.level3.lang.Type;

/**
 * Base interface for all IR nodes representing statements, i.e., things that
 * function as a complete operation
 */
public sealed interface IrExpression extends IrNode
    permits /* ... */ {

    /** Returns the data type of this expression */
    Type type();
}
