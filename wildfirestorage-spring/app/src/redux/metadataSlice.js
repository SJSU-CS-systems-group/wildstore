import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    metadataRecords: [],
    modalMetadata: null,
    descriptions: {"variables":[], "attributes": []},
}

export const metadataSlice = createSlice({
    name: "metadataSlice",
    initialState,
    reducers: {
        setMetadata: (state, action) => {
            state.metadataRecords = action.payload;
        },
        setDescriptions: (state, action) => {
            state.descriptions = action.payload;
        },
    }
});

export const { setMetadata, setDescriptions } = metadataSlice.actions;
export default metadataSlice.reducer;