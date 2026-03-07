import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    selectedRecord: null,
    locateTimestamp: null,
}

export const mapSlice = createSlice({
    name: "mapSlice",
    initialState,
    reducers: {
        setSelectedRecord: (state, action) => {
            state.selectedRecord = action.payload;
            state.locateTimestamp = Date.now();
        }
    }
});

export const { setSelectedRecord } = mapSlice.actions;
export default mapSlice.reducer;