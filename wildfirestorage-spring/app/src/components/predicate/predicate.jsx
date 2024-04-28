
import Autocomplete from '../autocomplete/autocomplete';
import { GoTrash, GoPlus } from 'react-icons/go';
import { useState, useEffect, useRef } from "react";

const Predicate = ({ component, onChange, onAdd, items, onToggle, onDelete }) => {

    const handleInputChange = (field, e) => {
        onChange(field, e.target.value);
    };

    const handleAddClick = () => {
        onAdd();
    };

    const handleDeleteClick = () => {
        onDelete();
    };

    return (
        <div className="join">
            <div className="flex-none">
                <button className="btn join-item" 
                  onClick={(e) => {
                    onToggle(e.target.value)
                  }}
                  value={component.fieldName}>{component.fieldName}
                </button>
            </div>
            <div className="flex-auto w-36">
                <Autocomplete value={component.varName} 
                  onChange={(e) => handleInputChange('varName', e)}
                  items={items} />
            </div>
            <select className="select select-bordered join-item flex-auto" 
              onChange={(e) => handleInputChange('statsValue', e)}
              value={component.statsValue}>
                <option hidden>stat</option>
                <option value="minValue">min</option>
                <option value="maxValue">max</option>
                <option value="average">avg</option>
            </select>
            <select className="select select-bordered join-item flex-auto" 
              onChange={(e) => handleInputChange('operator', e)}
              value={component.operator}>
                <option hidden>op</option>
                <option>{"="}</option>
                <option>{"<"}</option>
                <option>{"<="}</option>
                <option>{">"}</option>
                <option>{">="}</option>
            </select>
            <div className="flex-auto">
                <input
                    className="input input-bordered join-item w-28"
                    placeholder="value"
                    onChange={(e) => handleInputChange('fieldValue', e)}
                    value={component.fieldValue}
                />
            </div>
            <div className="flex-none">
                <button className="btn join-item" onClick={handleAddClick}><GoPlus /></button>
            </div>
            <div className="flex-none">
                <button className="btn join-item" onClick={handleDeleteClick}><GoTrash /></button>
            </div>
        </div>
    );
}

export default Predicate;