import { GoSearch, GoFilter, GoCodescanCheckmark, GoX } from 'react-icons/go';
import Filter from '../filter/filter';
import SearchResultContainer from '../searchResultContainer/searchResultContainer';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setMetadata } from '../../redux/metadataSlice';
import { setOpaqueToken } from '../../redux/userSlice';
import { setQueryCount } from '../../redux/filterSlice';
import Modal from '../modal/modal';

const Workspace = () => {

    const dispatch = useDispatch();
    const metadataRecords = useSelector(state => state.metadataReducer.metadataRecords)
    const query = useSelector(state => state.filterReducer.query)
    const limit = useSelector(state => state.filterReducer.limit)
    const offset = useSelector(state => state.filterReducer.offset)

    const [showModal, setShowModal] = useState(false);

    const getData = async () => {
        const response = await fetch("http://localhost:8080/api/metadata/search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({ "searchQuery": query, "excludeFields": ["variables", "globalAttributes"] , "limit": limit, "offset": offset}),
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        dispatch(setMetadata(d));
    }

    const getQueryCount = async () => {
        const response = await fetch("http://localhost:8080/api/metadata/search/count", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({ "searchQuery": query, "excludeFields": ["variables", "globalAttributes"] }),
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        dispatch(setQueryCount(d));
    }

    useEffect(() => {
        getData();
        getToken();
        getQueryCount();
    }, [])

    useEffect(() => {
        getData();
        getQueryCount();
    }, [query])

    useEffect(() => {
        getData();
    }, [limit, offset])

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
        if (response.redirected) {
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
                        <input type="text" placeholder="Search by file name" defaultValue={query} className="pr-3 pl-10 py-2 input input-bordered rounded-3xl w-full border-gray-100 shadow focus:outline-none text-black" />
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
                    <SearchResultContainer metadataRecords={metadataRecords} setShowModal={setShowModal}/>
                </div>
            </div>
            <Modal showModal={showModal} setShowModal={setShowModal} />
        </div>
    );
}

export default Workspace;