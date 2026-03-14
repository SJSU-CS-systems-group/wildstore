import React, { useEffect, useRef } from 'react';
import { useState } from "react";
import { useDispatch } from "react-redux";
import { addQuery } from '../../redux/filterSlice';
import Predicate from '../predicate/predicate';
import { useSelector } from 'react-redux';

const Filter = () => {

    const dispatch = useDispatch();

    const descriptions = useSelector(state => state.metadataReducer.descriptions);

    const variableList = descriptions.variables;

    const attributeList = descriptions.attributes;

    useEffect(() => {
        setItems(variableList)
    }, [variableList])


    const nextId = useRef(1);
    const makeComponent = () => ({
        id: nextId.current++,
        fieldName: 'VAR', varName: '', statsValue: 'stat', operator: 'op', fieldValue: ''
    });
    const [components, setComponents] = useState(() => [makeComponent()]);
    const [items, setItems] = useState([...variableList]);

    const handleAddComponent = (index) => {
        const newComponents = [...components];
        newComponents.splice(index + 1, 0, makeComponent());
        setComponents(newComponents);
    };

    const handleChange = (index, field, value) => {
        const updatedComponents = [...components];
        updatedComponents[index] = { ...components[index], [field]: value };
        setComponents(updatedComponents);
    };

    const handleToggle = (index, toggleVal) => {
        handleChange(index, "fieldName", toggleVal === "VAR" ? "ATTR" : "VAR")
        if (toggleVal === "VAR") {
            setItems(attributeList)
        } else {
            setItems(variableList)
        }
    }

    const handleDeleteComponent = (index) => {
        const updatedComponents = components.filter((_, i) => i !== index);
        setComponents(updatedComponents);
    };

    const initialPredicate = {
        fieldName: "VAR",
        varName: "",
        statsValue: "stat",
        operator: "op",
        fieldValue: ""
    };

    const handleSearch = async () => {
        let searchQuery = components.map((item, index) => {
            return `${item.fieldName}.${item.varName}.${item.statsValue} ${item.operator} ${item.fieldValue}`
        }).join(" AND ");
        dispatch(addQuery(searchQuery))
        setComponents([initialPredicate]);
        setItems(variableList);
    }

    return (
        <div className="flex flex-col content-between flex-wrap">
            <div className='flex flex-col gap-4'>
                {components.map((component, index) => (
                    <Predicate
                        key={component.id}
                        component={component}
                        onChange={(field, value) => handleChange(index, field, value)}
                        onAdd={() => handleAddComponent(index)}
                        onDelete={() => handleDeleteComponent(index)}
                        onToggle={(toggleVal) => handleToggle(index, toggleVal)}
                        items={items}
                        showDelete={components.length > 1}
                    />
                ))}
            </div>
            <button className="btn mt-4" onClick={handleSearch}>Search</button>
        </div>
    );
}
export default Filter;
