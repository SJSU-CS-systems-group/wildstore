import { useState } from "react";

const SimpleModal = ({showModal, closeModal, content}) => {
    return (
        <div>
            <dialog id="my_modal_1"  className={showModal? "modal modal-open": "modal"}>
                <div className="modal-box w-11/12 max-w-5xl">
                    {/* <h3 className="font-bold text-lg">Hello!</h3>
                    <p className="py-4">Press ESC key or click the button below to close</p> */}
                    {content}
                    <div className="modal-action">
                        <form method="dialog">
                            {/* if there is a button in form, it will close the modal */}
                            <button className="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" onClick={closeModal}>✕</button>
                        </form>
                    </div>
                </div>
            </dialog>
        </div>
    )
}

export default SimpleModal;