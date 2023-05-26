package com.sjsu.wildfirestorage;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

public class GlobalAttribute {
    public String attributeName;
    public String type;
    public Object value;
}
