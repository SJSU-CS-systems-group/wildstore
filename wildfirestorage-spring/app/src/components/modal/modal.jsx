import { useState } from "react";
import { useSelector } from "react-redux";

const Modal = ({ showModal, setShowModal }) => {

    const data = useSelector(state => state.metadataReducer.modalMetadata);
    
    return (
        <div>
            
            <dialog id="my_modal_3" className={showModal? "modal modal-open": "modal"}>
                <div className="modal-box w-11/12 max-w-5xl">
                    <form method="dialog">
                        {/* if there is a button in form, it will close the modal */}
                        <button className="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" onClick={() => setShowModal(false)}>✕</button>
                    </form>
                    <h3 className="font-bold text-lg">{data ? data.filePath : ""}</h3>
                    <p className="py-4">
                        {JSON.stringify(data)}
                        Press ESC key or click on ✕ button to close</p>
                </div>
            </dialog>
        </div>
    );
}

export default Modal;