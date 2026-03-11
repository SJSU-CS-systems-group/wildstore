import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    opaqueToken: "",
}

export const userSlice = createSlice({
    name: "userSlice",
    initialState,
    reducers: {
        setOpaqueToken: (state, action) => {
            state.opaqueToken = action.payload;
        },
    }
});

export const { setOpaqueToken } = userSlice.actions;
export default userSlice.reducer;