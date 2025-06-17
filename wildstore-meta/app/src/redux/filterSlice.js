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
            const newPage = parseInt(action.payload);
            const currentPage = state.currentPage;
            const currentSection = Math.floor(currentPage/5);

            if(newPage === 0) {
                state.currentPage = (currentSection - 1)*5 + 1;
            } else if(newPage === 6) {
                state.currentPage = (currentSection + 1)*5 + 1;
            } else {
                state.currentPage = newPage;
            }
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