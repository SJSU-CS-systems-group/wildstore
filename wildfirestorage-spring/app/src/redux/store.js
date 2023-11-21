import { combineReducers, configureStore } from "@reduxjs/toolkit";
import metadataSlice from "./metadataSlice";
import mapSlice from "./mapSlice";
import userSlice from "./userSlice";

export const store = configureStore({
    reducer: {
        metadataReducer: metadataSlice,
        mapReducer: mapSlice,
        userReducer: userSlice,
    },
});