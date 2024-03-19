import React, { useState, useEffect } from 'react';
import { GoHistory, GoTrash } from 'react-icons/go';
import SimpleModal from '../simpleModal/simpleModal';

const ShareLinks = () => {
    const [shareLinks, setShareLinks] = useState([{}]);
    const [showModal, setShowModal] = useState(false)
    const [content, setContent] = useState(null)
    const [count, setCount] = useState(0)
    const [currentPage, setCurrentPage] = useState(1)
    const [pageButtons, setPageButtons] = useState([])

    const getLinkData = async (limit, offset) => {
        let url = "/api/share-link/?" + "limit=" + limit + "&offset=" + offset
        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        setShareLinks(d);
    }

    const getLinkCount = async () => {

        const response = await fetch("/api/share-link/count", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        setCount(d);
        const pageSize = 100;
        const pb = []
        for (let i = 1; i <= (Math.ceil((count / pageSize))); i++) {
            pb.push(<button className={currentPage === i? "join-item btn btn-active" : "join-item btn"}
            onClick={() =>handlePageClick(i)}>{i}</button>)
        }
        setPageButtons(pb)
    }

    const handlePageClick = (index) => {
        setCurrentPage(index)
        getLinkData(100, index*100);
    }

    useEffect(() => {
        getLinkData(100, 0);
        getLinkCount();
    }, []);

    const handleDelete = async (index) => {
        const response = await fetch("/api/share-link/" + shareLinks[index].shareId, {
            method: "DELETE",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        if (d) {
            setShareLinks(shareLinks.filter((_, i) => i !== index));
            setShowModal(false)
        }
    };

    const handleDetailsContent = (index) => {
        let history = shareLinks[index].downloads;
        if (history) {

            let details = (
                <>
                    <h3 className="font-bold text-lg">Download History</h3>
                    <div className="overflow-x-auto">
                        <table className="table">
                            {/* head */}
                            <thead>
                                <tr>
                                    <th></th>
                                    <th>Downloaded By</th>
                                    <th>Time</th>
                                </tr>
                            </thead>
                            <tbody>
                                {history.map((h, i) => {
                                    return (
                                        <tr>
                                            <th>{i + 1}</th>
                                            <td>{h.downloadedBy}</td>
                                            <td>{h.dateTime}</td>
                                        </tr>
                                    )
                                })}
                                <tr className="bg-base-200">
                                    <th></th>
                                    <td><span className="font-bold">Total</span></td>
                                    <td><span className="font-bold">{history.length}</span></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </>
            )
            setContent(details);
        } else {
            setContent(<h3 className="font-bold text-lg">History not available!</h3>)
        }
        setShowModal(true)
    }

    const handleDeleteContent = (index) => {
        let deleteMsg = <div>
            <h3 className="font-bold text-lg">Confirmation</h3>
            <p>Are you sure you want to delete this link ({shareLinks[index].shareId})?</p>
            <div className="flex flex-row justify-center mt-12">
                <button onClick={() => handleDelete(index)} className="btn">Yes</button>
            </div>
        </div>
        setContent(deleteMsg);
        setShowModal(true)
    }

    return (
        <div className="col-span-11 p-6">
            <h1 className="text-2xl font-bold mb-4">My Share Links</h1>
            <div className="overflow-x-auto">
                <table className="w-full table-auto">
                    <thead>
                        <tr>
                            <th className="px-4 py-2">Share Id</th>
                            <th className="px-4 py-2">Created At</th>
                            <th className="px-4 py-2">File Digest</th>
                            <th className="px-4 py-2">Expiry</th>
                            <th className="px-4 py-2">Downloads</th>
                            <th className="px-4 py-2">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {shareLinks && shareLinks.map((link, index) => (
                            <tr key={index}>
                                <td className="border px-4 py-2">{link.shareId}</td>
                                <td className="border px-4 py-2">{link.createdAt}</td>
                                <td className="border px-4 py-2">{link.fileDigest}</td>
                                <td className="border px-4 py-2">{link.expiry? (new Date(link.expiry)).toString() : ""}</td>
                                <td className="border px-4 py-2">{link.downloads?.length}</td>
                                <td className="border px-4 py-2">
                                    <button onClick={() => {
                                        handleDetailsContent(index);
                                    }
                                    } className="btn"><GoHistory /></button>
                                    <button onClick={() => handleDeleteContent(index)} className="btn"><GoTrash /></button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
                <div className="flex flex-row justify-center mt-6">
                    <div className="join">
                        {
                            pageButtons
                        }
                    </div>
                </div>
            </div>
            <SimpleModal showModal={showModal} closeModal={() => setShowModal(false)} content={content} />
        </div>
    );
}

export default ShareLinks;