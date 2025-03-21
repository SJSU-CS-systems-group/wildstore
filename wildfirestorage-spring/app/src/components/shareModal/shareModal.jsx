import { useState, useRef } from "react";
import { GoX } from "react-icons/go";

const ShareModal = ({ digestString, showModal, closeModal, generateShareLink, generatedLink }) => {

    const [emailAddresses, setEmailAddresses] = useState([]);
    const [error, setError] = useState("");
    const [validFor, setValidFor] = useState("");
    const inputRef = useRef();
    const validForRef = useRef();

    const handleInputChange = (event) => {
        const inputEmail = event.target.value;
        if (!inputEmail.toLowerCase()
            .match(
                /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.?)+[a-zA-Z]{2,}))$/
            )) {
            setError("input-error");
            return;
        }
        setError("");
        if (event.key === 'Enter' && inputEmail !== "") {
            if (!emailAddresses.includes(inputEmail)) {
                setEmailAddresses([...emailAddresses, inputEmail]);
                inputRef.current.value = ""
            }
        }
    }

    const handleValidForChange = (event) => {
        setValidFor(event.target.value);
        validForRef.current.classList.remove("select-error");
    }

    const handleDeleteEmail = (index) => {
        let emailAddrs = [...emailAddresses];
        emailAddrs.splice(index, 1);
        setEmailAddresses(emailAddrs);
    }

    const handleSubmit = (event) => {
        if (!emailAddresses || emailAddresses.length === 0) {
            if (error !== "" || !inputRef.current.value) {
                setError("input-error")
                return;
            } else if (!validFor) {
                validForRef.current.classList.add("select-error");
                return;
            }
            generateShareLink([inputRef.current.value], validFor);
        } else if (!validFor) {
            validForRef.current.classList.add("select-error");
        } else {
            generateShareLink(emailAddresses, validFor);
        }
    }

    return (
        <>
            <dialog id="my_modal_3" className={showModal ? "modal modal-open" : "modal"}>
                <div className="modal-box w-11/12 max-w-5xl">
                    <form method="dialog">
                        <button className="btn btn-sm btn-circle btn-ghost absolute right-2 top-2" onClick={closeModal}>✕</button>
                    </form>
                    <h3 className="font-bold text-lg">Share</h3>
                    {generatedLink ? generatedLink :
                        <div className="flex flex-col gap-4 py-4 items-center">
                            <input ref={inputRef} type="text" placeholder="Enter email adresses to share" className={"input input-bordered w-full max-w-lg " + error} onKeyDown={(event) => handleInputChange(event)} />
                            <div className="flex flex-wrap gap-2">
                                {emailAddresses.map((addr, i) => {
                                    return (
                                        <div key={i} className="badge badge-lg gap-2 border-gray-400 h-10 px-4">
                                            {addr}
                                            <GoX size={14} className="w-3.5" onClick={(event) => handleDeleteEmail(i)} />
                                        </div>
                                    );
                                })}
                            </div>
                            <select className="select select-bordered w-full max-w-xs" ref={validForRef} onChange={(e) => handleValidForChange(e)}>
                                <option hidden>Valid for</option>
                                <option value="day">1 day</option>
                                <option value="week">1 week</option>
                                <option value="month">1 month</option>
                                <option value="year">1 year</option>
                            </select>
                            <button className="btn bg-primary text-white hover:bg-gray-600 hover:bg-opacity-40" onClick={(event) => handleSubmit(event)}>Share</button>
                        </div>
                    }
                </div>
            </dialog>
        </>
    )
}

export default ShareModal;