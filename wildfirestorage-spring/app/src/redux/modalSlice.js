import { createSlice } from "@reduxjs/toolkit";

const initialState = {
    data: "",
    showModal: false,
    header: "",
}

export const modalSlice = createSlice({
    name: "modalSlice",
    initialState,
    reducers: {
        setModalData: (state, action) => {
            state.data = action.payload.data;
            state.header = action.payload.header;
        },
        setShowModal: (state, action) => {
            state.showModal = action.payload;
        },
    }
});

export const { setModalData, setShowModal } = modalSlice.actions;
export default modalSlice.reducer;