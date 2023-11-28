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
            state.metadataRecords = action.payload;
        },
    }
});

export const { setMetadata } = metadataSlice.actions;
export default metadataSlice.reducer;