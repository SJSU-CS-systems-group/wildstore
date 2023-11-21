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
        setModalMetadata: (state, action) => {
            state.modalMetadata = action.payload;
        }
    }
});

export const { setMetadata, setModalMetadata } = metadataSlice.actions;
export default metadataSlice.reducer;