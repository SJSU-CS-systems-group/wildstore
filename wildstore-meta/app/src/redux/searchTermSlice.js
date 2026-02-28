import {createSlice} from "@reduxjs/toolkit";

const initialState = {
    searchTerm: "",
}

export const searchTermSlice = createSlice({
    name: "searchTermSlice",
    initialState,
    reducers: {
        setSearchTerm: (state, action) => {
            state.searchTerm = action.payload;
        },
    }
});

export const { setSearchTerm } = searchTermSlice.actions;
export default searchTermSlice.reducer;