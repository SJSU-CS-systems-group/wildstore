import {createSlice} from "@reduxjs/toolkit";

const initialState = {
    query: [],
    queryCount: 1,
    currentPage: 1,
    limit: 10,
    offset: 0,
}

export const filterSlice = createSlice({
    name: "filterSlice",
    initialState,
    reducers: {
        addQuery: (state, action) => {
            let newQuery = [...state.query]
            newQuery.push(action.payload)
            state.query = newQuery;
        },
        deleteQuery: (state, action) => {
            state.query.splice(action.payload, 1);
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

export const { addQuery, deleteQuery, setQueryCount, setCurrentPage, setLimit, setOffset } = filterSlice.actions;
export default filterSlice.reducer;