import SearchResult from '../search-result/searchResult';
import { useSelector } from 'react-redux';
import { useDispatch } from 'react-redux';
import { setCurrentPage } from '../../redux/filterSlice';
import { GoTriangleLeft, GoTriangleRight } from 'react-icons/go';

const SearchResultContainer = ({ metadataRecords, setShowModal }) => {

    const queryCount = useSelector(state => state.filterReducer.queryCount);
    const currentPage = useSelector(state => state.filterReducer.currentPage);
    const pageSize = useSelector(state => state.filterReducer.limit);
    const dispatch = useDispatch();
    
    const pageCount = metadataRecords.length !== 0? Math.ceil(queryCount/ pageSize) : 1;

    const handlePageClick = (event) => {
        dispatch(setCurrentPage(event.target.value));
    }

    const pageButtons = [];
    const sectionNum = Math.floor((currentPage - 1)/5);
    const lastSection = Math.floor(pageCount/5);
    if(sectionNum === 0) {
        for(let i=1; i<=Math.min(pageCount, 5); i++) {
            pageButtons.push(<button className={currentPage === i? "join-item btn btn-active": "join-item btn"} onClick={handlePageClick} value={i}>{i}</button> );
        }
        if(pageCount > 5) {
            pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value="6"><GoTriangleRight size={20}/></button> );
        }
    } else if (sectionNum === lastSection) {
        pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value="0"><GoTriangleLeft size={20}/></button> );
        for(let i=(sectionNum*5 + 1); i<pageCount; i++) {
            pageButtons.push(<button className={currentPage === i? "join-item btn btn-active": "join-item btn"} onClick={handlePageClick} value={i}>{i}</button> );
        }
    } else {
        pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value="0"><GoTriangleLeft size={20}/></button> );
        for(let i=(sectionNum*5 + 1); i<(sectionNum*5 + 6); i++) {
            pageButtons.push(<button className={currentPage === i? "join-item btn btn-active": "join-item btn"} onClick={handlePageClick} value={i}>{i}</button> );
        }
        pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value="6"><GoTriangleRight size={20}/></button> );
    }
    

    return (
        <div className='flex flex-col gap-3'>
            <div className="flex flex-col gap-2">
                {metadataRecords.map((metadataRecord, i) => <SearchResult key={metadataRecord.digestString} metadataRecord={metadataRecord} setShowModal={setShowModal} />)}
            </div>
            <div className="flex flex-wrap self-center join">
                {
                    pageButtons
                }
            </div>
        </div>
    );
}

export default SearchResultContainer;