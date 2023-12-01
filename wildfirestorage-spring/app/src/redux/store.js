import { combineReducers, configureStore } from "@reduxjs/toolkit";
import metadataSlice from "./metadataSlice";
import mapSlice from "./mapSlice";
import userSlice from "./userSlice";
import filterSlice from "./filterSlice";
import modalSlice from "./modalSlice";
import searchTermSlice from "./searchTermSlice";

export const store = configureStore({
    reducer: {
        metadataReducer: metadataSlice,
        mapReducer: mapSlice,
        userReducer: userSlice,
        filterReducer: filterSlice,
        modalReducer: modalSlice,
        searchTermReducer: searchTermSlice,
    },
});