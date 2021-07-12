package io.jmix.dataimport.model.configuration;

import io.jmix.core.metamodel.datatype.impl.EnumClass;

import javax.annotation.Nullable;


public enum DuplicateEntityPolicy implements EnumClass<String> {

    SKIP("SKIP"),
    UPDATE("UPDATE"),
    ABORT("ABORT");

    private String id;

    DuplicateEntityPolicy(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static DuplicateEntityPolicy fromId(String id) {
        for (DuplicateEntityPolicy at : DuplicateEntityPolicy.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}