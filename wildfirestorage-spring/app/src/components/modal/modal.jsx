import { useSelector } from "react-redux";
import { useDispatch } from "react-redux";
import { setShowModal } from "../../redux/modalSlice";

const Modal = () => {

    const data = useSelector(state => state.modalReducer.data);
    const header = useSelector(state => state.modalReducer.header);
    const showModal = useSelector(state => state.modalReducer.showModal);
    const dispatch = useDispatch();

    const dataObject = data ? JSON.parse(data) : {};

    return (
        <div>
            <dialog id="my_modal_3" className={showModal? "modal modal-open": "modal"}>
                <div className="modal-box w-11/12 max-w-5xl">
                    <form method="dialog">
                        {/* if there is a button in form, it will close the modal */}
                        <button className="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" onClick={() => dispatch(setShowModal(false))}>✕</button>
                    </form>
                    <h3 className="font-bold text-lg">{dataObject.fileName}</h3>
                    <p className="pt-3">{dataObject.digestString && `Digest String: ${dataObject.digestString}`}</p>
                    <p className="pt-3">{dataObject.domain != null&& `Domain: ${dataObject.domain}`}</p>
                    <p className="pt-3">{dataObject.filePath && `File Paths: ${dataObject.filePath.join(", ")}`}</p>
                    <p className="pt-3">{dataObject.variables && `Variables: `}</p>
                    <p className="pt-3">{dataObject.globalAttributes && `Global Attributes: `}</p>
                </div>
            </dialog>
        </div>
    );
}

export default Modal;