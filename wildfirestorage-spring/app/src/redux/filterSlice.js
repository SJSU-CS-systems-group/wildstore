import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    query: "",
    queryCount: 1,
    currentPage: 1,
    limit: 10,
    offset: 0,
}

export const filterSlice = createSlice({
    name: "filterSlice",
    initialState,
    reducers: {
        setQuery: (state, action) => {
            state.query = action.payload;
        },
        setQueryCount: (state, action) => {
            state.queryCount = action.payload;
        },
        setCurrentPage: (state, action) => {
            state.currentPage = action.payload;
            state.offset = state.limit*(state.currentPage - 1)
        },
        setLimit: (state, action) => {
            state.limit = action.payload;
        },
        setOffset: (state, action) => {
            state.offset = action.payload;
        },
    }
});

export const { setQuery, setQueryCount, setCurrentPage, setLimit, setOffset } = filterSlice.actions;
export default filterSlice.reducer;