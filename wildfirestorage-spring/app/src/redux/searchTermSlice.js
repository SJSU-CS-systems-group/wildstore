import {createSlice} from "@reduxjs/toolkit";

const initialState = {
    searchTerm: "",
}

export const searchTermSlice = createSlice({
    name: "searchTermSlice",
    initialState,
    reducers: {
        setSearchTerm: (state, action) => {
            state.query = action.payload;
        },
    }
});

export const { setSearchTerm } = searchTermSlice.actions;
export default searchTermSlice.reducer;