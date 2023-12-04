import SearchResult from '../search-result/searchResult';
import { useSelector } from 'react-redux';
import { useDispatch } from 'react-redux';
import { setCurrentPage } from '../../redux/filterSlice';

const SearchResultContainer = ({ metadataRecords, setShowModal }) => {

    const queryCount = useSelector(state => state.filterReducer.queryCount);
    const currentPage = useSelector(state => state.filterReducer.currentPage);
    const pageSize = useSelector(state => state.filterReducer.limit);
    const dispatch = useDispatch();
    
    const pageCount = metadataRecords.length !== 0? Math.ceil(queryCount/ pageSize) : 1;

    const handlePageClick = (event) => {
        dispatch(setCurrentPage(event.target.value));
    }
    

    return (
        <div className='flex flex-col gap-3'>
            <div className="flex flex-col gap-2">
                {metadataRecords.map((metadataRecord, i) => <SearchResult key={metadataRecord.digestString} metadataRecord={metadataRecord} setShowModal={setShowModal} />)}
            </div>
            <div className="flex flex-wrap self-center join">
                {
                    [...Array(pageCount)].map((x, i) => 
                    <button key={i} className={currentPage === (i+1)? "join-item btn btn-active": "join-item btn"} onClick={handlePageClick} value={i+1}>{i+1}</button> )
                }
            </div>
        </div>
    );
}

export default SearchResultContainer;