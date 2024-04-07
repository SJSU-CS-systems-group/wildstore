import { GoPaperAirplane, GoDownload, GoLocation } from 'react-icons/go';
import { useDispatch } from 'react-redux';
import { setSelectedRecord } from '../../redux/mapSlice';
import { setModalData } from '../../redux/modalSlice';
import { useSelector } from 'react-redux';
import { setShowModal } from '../../redux/modalSlice';
import { useState } from 'react';
import ShareModal from '../shareModal/shareModal';

const SearchResult = ({ metadataRecord }) => {

    const [showShareModal, setShowShareModal] = useState(false);
    const [shareModalData, setShareModalData] = useState(null);
    const [generatedLink, setGeneratedLink] = useState("");

    const dispatch = useDispatch();

    const locateOnMap = () => {
        dispatch(setSelectedRecord(metadataRecord))
    }

    const getData = async () => {
        const response = await fetch("/api/metadata/search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({ "searchQuery": `digestString = '${metadataRecord.digestString}'` }),
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        dispatch(setModalData({ "data": JSON.stringify(d[0]), "header": d[0].filePath[0] }));
    }

    const fetchDetails = () => {
        dispatch(setShowModal(true));
        getData();
    }

    const token = useSelector(state => state.userReducer.opaqueToken)

    const download = async () => {
        const response = await fetch("/api/file/" + metadataRecord.digestString, {
            method: 'GET',
            headers: new Headers({
                'Authorization': 'Bearer ' + token
            }),
        });
        if (response.redirected) {
            const fileResponse = await fetch(response.url, {
                method: 'GET',
                headers: new Headers({
                    'Authorization': 'Bearer ' + token
                }),
            });
            const blob = await fileResponse.blob();
            var url = window.URL.createObjectURL(blob);
            var a = document.createElement('a');
            a.href = url;
            a.download = metadataRecord.filePath[0].split("/").pop().split("\\").pop();
            document.body.appendChild(a); // we need to append the element to the dom -> otherwise it will not work in firefox
            a.click();
            a.remove();  //afterwards we remove the element again  
        }
    }

    const generateShareLink = async (emailAddresses, validFor) => {
        const response = await fetch("/api/share-link/create", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({
                "fileDigest": metadataRecord.digestString,
                emailAddresses,
                validFor
            }),
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.text();
        setGeneratedLink(d);
    }

    return (
        <div className="card bg-base-100 shadow-xl w-0 min-w-full hover:shadow-2xl">
            <div className="card-body">
                <h3 className="card-title text-lg hover:cursor-pointer break-all">{metadataRecord.fileName[0]}</h3>
                <p className="break-words">{metadataRecord.filePath[0]}</p>
                <div className="card-actions justify-end items-center">
                    <div><a className="link link-primary" onClick={fetchDetails}>Details</a></div>
                    <div className='grow'></div>
                    <div className='tooltip' data-tip="Locate on Map">
                        <button className="btn btn-square bg-transparent border-none" onClick={locateOnMap}>
                            <GoLocation size={20} />
                        </button>
                    </div>
                    <div className='tooltip' data-tip="Send">
                        <button className="btn btn-square bg-transparent border-none" onClick={() => { setGeneratedLink(""); setShowShareModal(true) }}>
                            <GoPaperAirplane size={20} />
                        </button>
                    </div>
                    <div className='tooltip' data-tip="Download">
                        <button className="btn btn-square bg-transparent border-none" onClick={download}>
                            <GoDownload size={20} />
                        </button>
                    </div>
                </div>
            </div>
            <div>
                <ShareModal digestString={""} showModal={showShareModal} closeModal={() => { setShowShareModal(false) }} generateShareLink={generateShareLink} generatedLink={generatedLink} />
            </div>
        </div >
    );
}

export default SearchResult;