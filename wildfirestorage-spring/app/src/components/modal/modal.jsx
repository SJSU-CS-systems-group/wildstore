import { useSelector } from "react-redux";
import { useDispatch } from "react-redux";
import { setShowModal } from "../../redux/modalSlice";

const Modal = () => {

    const data = useSelector(state => state.modalReducer.data);
    const header = useSelector(state => state.modalReducer.header);
    const showModal = useSelector(state => state.modalReducer.showModal);
    const dispatch = useDispatch();
    
    return (
        <div>
            <dialog id="my_modal_3" className={showModal? "modal modal-open": "modal"}>
                <div className="modal-box w-11/12 max-w-5xl">
                    <form method="dialog">
                        {/* if there is a button in form, it will close the modal */}
                        <button className="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" onClick={() => dispatch(setShowModal(false))}>✕</button>
                    </form>
                    <h3 className="font-bold text-lg">{header}</h3>
                    <p className="py-4">
                        {data}
                    </p>
                </div>
            </dialog>
        </div>
    );
}

export default Modal;