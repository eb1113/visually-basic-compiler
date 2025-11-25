package edu.westminsteru.cmpt355.level3.lang;

public sealed interface ScalarType extends Type
    permits BuiltInType, RecordType {
}
