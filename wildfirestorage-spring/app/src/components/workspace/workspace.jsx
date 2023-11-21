import { GoSearch, GoFilter, GoCodescanCheckmark, GoX } from 'react-icons/go';
import Filter from '../filter/filter';
import SearchResult from '../search-result/searchResult';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setMetadata } from '../../redux/metadataSlice';
import { setOpaqueToken } from '../../redux/userSlice';
import Modal from '../modal/modal';

const Workspace = () => {

    // const [records, setRecords] = useState([]);
    const dispatch = useDispatch();
    const metadataRecords = useSelector(state => state.metadataReducer.metadataRecords)
    
    const [showModal, setShowModal] = useState(false);

    const getData = async () => {
        const response = await fetch("http://localhost:8080/api/metadata/search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({"searchQuery":"VAR.NorthWind.minValue > 0.0", "excludeFields": ["variables", "globalAttributes"]}),
            credentials: "include",
            redirect: "follow",
        });
        if(response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        dispatch(setMetadata(d));
        console.log("data", d)
        // setRecords(d)
    }

    useEffect(() => {
        getData();
        getToken();
    }, [])

    const getToken = async () => {
        const response = await fetch("http://localhost:8080/token", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            credentials: "include",
            redirect: "follow",
        });
        if(response.redirected) {
            document.location = response.url;
        }
        let d = await response.text();
        dispatch(setOpaqueToken(d.substring(6)));
        console.log("tokennnn", d)
    }

    return (
        <div className="divide-y divide-slate-200 h-full">
            <div className='p-6'>
                <div className='flex justify-center pb-4'>
                    <div className='flex items-center text-gray-400 w-9/12 relative'>
                        <GoSearch size={20} className='absolute ml-3 pointer-events-none' />
                        <input type="text" placeholder="Search by file name" className="pr-3 pl-10 py-2 input input-bordered rounded-3xl w-full border-gray-100 shadow focus:outline-none" />
                    </div>
                </div>
                <div className='py-3 flex flex-wrap gap-1'>
                    <div className="badge gap-2 cursor-pointer">
                        <GoX size={14} />
                        info
                    </div>
                    <div className="badge gap-2 cursor-pointer">
                        <GoX size={14} />
                        {"FMOIST > 10"}
                    </div>
                    <div className="badge gap-2 cursor-pointer">
                        <GoX size={14} />
                        {"NorthWestWind <= 60"}
                    </div>
                    <div className="badge gap-2 cursor-pointer">
                        <GoX size={14} />
                        info
                    </div>
                    <div className="badge gap-2 cursor-pointer">
                        <GoX size={14} />
                        info
                    </div>
                    <div className="badge gap-2 cursor-pointer">
                        <GoX size={14} />
                        info
                    </div>
                </div>
            </div>
            <div className="collapse collapse-arrow rounded-none">
                <input type="checkbox" />
                <div className="collapse-title text-xl font-medium">
                    <div className='flex gap-4 items-center'>
                        <GoFilter size={20} />
                        Filters
                    </div>
                </div>
                <div className="collapse-content">
                    <Filter />
                </div>
            </div>
            <div className="collapse collapse-arrow rounded-none">
                <input type="checkbox" />
                <div className="collapse-title text-xl font-medium">
                    <div className='flex gap-4 items-center'>
                        <GoCodescanCheckmark size={20} />
                        Search Results
                    </div>
                </div>
                <div className="collapse-content">
                    <div className='flex flex-col gap-3'>
        <div className="flex flex-col gap-2">
                        {metadataRecords.map((metadataRecord, i) => <SearchResult key={metadataRecord.digestString} metadataRecord={metadataRecord} setShowModal={setShowModal}/>)}
                        
        </div>
                        <div className="self-center join">
                            <button className="join-item btn">1</button>
                            <button className="join-item btn">2</button>
                            <button className="join-item btn btn-disabled">...</button>
                            <button className="join-item btn">99</button>
                            <button className="join-item btn">100</button>
                        </div>
                    </div>
                </div>
            </div>
            <Modal showModal={showModal} setShowModal={setShowModal}/>
            <div></div>
        </div>
    );
}

export default Workspace;