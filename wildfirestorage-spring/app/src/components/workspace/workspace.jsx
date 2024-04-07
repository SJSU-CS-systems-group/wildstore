import { GoSearch, GoFilter, GoCodescanCheckmark, GoX } from 'react-icons/go';
import Filter from '../filter/filter';
import SearchResultContainer from '../searchResultContainer/searchResultContainer';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { setMetadata, setDescriptions } from '../../redux/metadataSlice';
import { addQuery, deleteQuery, setQueryCount } from '../../redux/filterSlice';
import Modal from '../modal/modal';
import { setSearchTerm } from "../../redux/searchTermSlice";

const Workspace = () => {

    const dispatch = useDispatch();
    const metadataRecords = useSelector(state => state.metadataReducer.metadataRecords)
    const query = useSelector(state => state.filterReducer.query)
    const limit = useSelector(state => state.filterReducer.limit)
    const offset = useSelector(state => state.filterReducer.offset)
    const search = useSelector(state => state.searchTermReducer.searchTerm)

    const [openSearchResults, setOpenSearchResults] = useState(true)
    const [showModal, setShowModal] = useState(false);

    const getData = async () => {
        const builtQuery = (query.length !== 0) ? query.join(" AND ") : "";

        const response = await fetch("/api/metadata/search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({ "searchQuery": builtQuery, "excludeFields": ["variables", "globalAttributes"], "limit": limit, "offset": offset }),
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
        const builtQuery = (query.length !== 0) ? query.join(" AND ") : "";

        const response = await fetch("/api/metadata/search/count", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({ "searchQuery": builtQuery, "excludeFields": ["variables", "globalAttributes"] }),
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        dispatch(setQueryCount(d));
    }

    const getDescriptions = async () => {
        const response = await fetch("/api/metadata/description", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/plain, application/json",
            },
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.text();
        dispatch(setDescriptions(JSON.parse(d)));
    }

    useEffect(() => {
        getData();
        getQueryCount();
        getDescriptions();
    }, [])

    useEffect(() => {
        getData();
        getQueryCount();
    }, [query])

    useEffect(() => {
        getData();
    }, [limit, offset])



    const handleDeleteFilter = (event) => {
        const deleteID = event.target.parentNode.id
        dispatch(deleteQuery(deleteID))
    }
    const handleSearch = (event) => {
        const searchTerm = event.target.value;
        if (event.key === 'Enter' && searchTerm !== "") {
            dispatch(setSearchTerm(searchTerm)) //Set global state for search term
            const searchQuery = `(fileName = '${searchTerm}' OR digestString = '${searchTerm}')`
            dispatch(addQuery(searchQuery)) //Add to the filters

            const searchBar = event.target; //Reset the search bar to empty
            searchBar.value = ""
        }
    }
    const handleOpenSearchResult = () => {
        setOpenSearchResults(!openSearchResults)
    }

    return (
        <div className="divide-y divide-slate-200 h-full">
            <div className='p-6'>
                <div className='flex justify-center pb-4'>
                    <div className='flex items-center text-gray-400 w-full relative'>
                        <GoSearch size={20} className='absolute ml-3 pointer-events-none' />
                        <input type="text" placeholder="Search by file name or digest"
                            onKeyDown={handleSearch}
                            className="text-black pr-3 pl-10 py-2 input input-bordered rounded-3xl w-full border-gray-100 shadow focus:outline-none"
                        />
                    </div>
                </div>
                <div className='py-3 flex flex-wrap gap-1'>
                    {query &&
                        query.map((item, i) =>
                            <div id={i} key={i} className="w-full h-full badge gap-2 cursor-pointer">
                                <GoX size={14} className="w-3.5" onClick={handleDeleteFilter} />
                                <p className="w-full">{item}</p>
                            </div>
                        )}
                </div>
            </div>
            <div className="h-fit">
                <div className="collapse collapse-arrow rounded-none">
                    <input type="checkbox" defaultChecked />
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
                
            </div>
            <div className="collapse collapse-arrow rounded-none">
                <input checked={openSearchResults} onChange={handleOpenSearchResult} type="checkbox" />
                <div className="collapse-title text-xl font-medium">
                    <div className='flex gap-4 items-center'>
                        <GoCodescanCheckmark size={20} />
                        Search Results
                    </div>
                </div>
                <div className="collapse-content">
                    <SearchResultContainer metadataRecords={metadataRecords} setShowModal={setShowModal} />
                </div>
            </div>
            {showModal && <Modal showModal={showModal} setShowModal={setShowModal} />}
        </div>
    );
}

export default Workspace;