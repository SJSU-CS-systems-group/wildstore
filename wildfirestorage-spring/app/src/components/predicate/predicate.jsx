
import Autocomplete from '../autocomplete/autocomplete';
import { GoTrash, GoPlus } from 'react-icons/go';
import { useState, useEffect } from "react";

const Predicate = ({items, toggle, addHandler, index}) => {
    const [value, setValue] = useState("");
    const [fieldName, setFieldName] = useState("Field");
    const [varName, setVarName] = useState("Name");
    const [statsValue, setStatsValue] = useState("Stats")
    const [operator, setOperator] = useState("Operator")
    const [fieldValue, setFieldValue] = useState("");
    const [warning, setWarning] = useState("");
    const [nameList, setNameList] = useState([{ "value": "Select a Field First", "label": "Select a Field First" }]);
    const [filteredItems, setFilteredItems] = useState([]);

    useEffect(() => {
        //if the val changes, we filter items so that it can be filtered. and set it as new state
        const newItems = items
            .filter((p) => p.toLowerCase().includes(value.toLowerCase()))
            .sort();
            setFilteredItems(newItems);
    }, [items, value]);

    return (
        <div className="join">
            <div className="flex-none">
                <button className="btn join-item" onClick={toggle}>VAR</button>
            </div>
            <div className="flex-auto w-36">
                <Autocomplete value={value} onChange={setValue} items={filteredItems} />
            </div>
            <select className="select select-bordered join-item flex-auto">
                <option disabled selected hidden>stat</option>
                <option>min</option>
                <option>max</option>
                <option>avg</option>
            </select>
            <select className="select select-bordered join-item flex-auto">
                <option disabled selected hidden>op</option>
                <option>{"="}</option>
                <option>{"<"}</option>
                <option>{"<="}</option>
                <option>{">"}</option>
                <option>{">="}</option>
            </select>
            <div className="flex-auto">
                <input className="input input-bordered join-item w-28" placeholder="value" />
            </div>
            <div className="flex-none">
                <button className="btn join-item" onClick={addHandler}><GoPlus /></button>
            </div>
            <div className="flex-none">
                <button className="btn join-item"><GoTrash/></button>
            </div>
        </div>
    );
}

export default Predicate;