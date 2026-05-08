import { useDispatch, useSelector } from "react-redux";
import { setShowModal } from "../../redux/modalSlice";
import MetadataDetails from "../metadata-details/metadataDetails";

const Modal = () => {

    const data = useSelector(state => state.modalReducer.data);
    const header = useSelector(state => state.modalReducer.header);
    const showModal = useSelector(state => state.modalReducer.showModal);
    const dispatch = useDispatch();

    const dataObject = data ? JSON.parse(data) : {};
    return (
        <div>
            <dialog id="detail-modal" className={showModal? "modal modal-open": "modal"}>
                <div className="modal-box w-11/12 max-w-5xl">
                    <form method="dialog">
                        {/* if there is a button in form, it will close the modal */}
                        <button className="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" onClick={() => dispatch(setShowModal(false))}>✕</button>
                    </form>
                    <MetadataDetails record={dataObject} header={header} />
                </div>
            </dialog>
        </div>
    );
}

export default Modal;