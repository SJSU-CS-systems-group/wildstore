import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    selectedRecord: {}
}

export const mapSlice = createSlice({
    name: "mapSlice",
    initialState,
    reducers: {
        setSelectedRecord: (state, action) => {
            state.selectedRecord = action.payload;
        }
    }
});

export const { setSelectedRecord } = mapSlice.actions;
export default mapSlice.reducer;