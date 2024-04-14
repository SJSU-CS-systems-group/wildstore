import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    metadataRecords: [],
    modalMetadata: null,
}

export const metadataSlice = createSlice({
    name: "metadataSlice",
    initialState,
    reducers: {
        setMetadata: (state, action) => {
            let recordsWithAppliedFilters = [];
            const {metadataPayload, filterQuery} = action.payload;
            metadataPayload.forEach(record => {
                recordsWithAppliedFilters.push(fetchFilterVarAndAtrr(record, filterQuery));
            })
            state.metadataRecords = recordsWithAppliedFilters;
        },
    }
});


const fetchFilterVarAndAtrr = (metadataRecord, filterQuery) => {
    let variableValue = 0;
    let appliedFilterValues = [];
    let variablesAdded = [];

    // Loop through each filter query
    filterQuery.forEach(query => {
        const [filterStr, operator, value] = query.split(' ');
        const [type, variableName, stat] = filterStr.split('.');

        if (!variablesAdded.includes(variableName)) {
            variablesAdded.push(variableName);
        } else {
            return;
        }

        // Check if it's a variable or attribute
        if (type === 'VAR') {
            const variable = metadataRecord.variables.find(variable => variable.variableName === variableName);
            if (stat === 'minValue') {
                variableValue = variable.minValue;
            } else if (stat === 'maxValue') {
                variableValue = variable.maxValue;
            } else {
                variableValue = variable.average;
            }
            const renderString = variableName + " = " + variableValue;
            appliedFilterValues.push(renderString);
        } else if (type === 'ATTR') {
            /* TO-DO : After structure for Attribute search is defined, implement this block */
        }
        metadataRecord = {...metadataRecord, appliedFilterValues};
    });
    return metadataRecord;
}

export const { setMetadata } = metadataSlice.actions;
export default metadataSlice.reducer;