import { GoTriangleLeft, GoTriangleRight } from 'react-icons/go';
import { useDispatch, useSelector } from 'react-redux';
import { setCurrentPage, setLimit } from '../../redux/filterSlice';
import SearchResult from '../search-result/searchResult';

const SearchResultContainer = ({ metadataRecords }) => {

    const queryCount = useSelector(state => state.filterReducer.queryCount);
    const currentPage = useSelector(state => state.filterReducer.currentPage);
    const pageSize = useSelector(state => state.filterReducer.limit);
    const dispatch = useDispatch();

    const handleLimitChange = (e) => {
        dispatch(setLimit(Number(e.target.value)));
    };
    
    const pageCount = metadataRecords.length !== 0? Math.ceil(queryCount/ pageSize) : 1;

    const handlePageClick = (event) => {
        dispatch(setCurrentPage(event.currentTarget.value));
    }

    const pageButtons = [];
    const sectionNum = Math.floor((currentPage - 1)/5);
    const lastSection = Math.floor(pageCount/5);
    if(sectionNum === 0) {
        for(let i=1; i<=Math.min(pageCount, 5); i++) {
            pageButtons.push(<button className={currentPage === i? "join-item btn btn-active": "join-item btn"} onClick={handlePageClick} value={i}>{i}</button> );
        }
        if(pageCount > 5) {
            pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value={6}><GoTriangleRight size={20}/></button> );
        }
    } else if (sectionNum === lastSection) {
        pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value={0}><GoTriangleLeft size={20}/></button> );
        for(let i=(sectionNum*5 + 1); i<pageCount; i++) {
            pageButtons.push(<button className={currentPage === i? "join-item btn btn-active": "join-item btn"} onClick={handlePageClick} value={i}>{i}</button> );
        }
    } else {
        pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value={0}><GoTriangleLeft size={20}/></button> );
        for(let i=(sectionNum*5 + 1); i<(sectionNum*5 + 6); i++) {
            pageButtons.push(<button className={currentPage === i? "join-item btn btn-active": "join-item btn"} onClick={handlePageClick} value={i}>{i}</button> );
        }
        pageButtons.push(<button className="join-item btn" onClick={handlePageClick} value={6}><GoTriangleRight size={20}/></button> );
    }
    

    return (
        <div className='flex flex-col gap-3'>
            <div className="flex flex-col gap-2">
                {metadataRecords.map((metadataRecord) => <SearchResult key={metadataRecord.digestString} metadataRecord={metadataRecord} />)}
            </div>
            <div className="flex items-center justify-center gap-4">
                <div className="join">
                    {pageButtons}
                </div>
                <select className="select select-bordered select-sm" value={pageSize} onChange={handleLimitChange}>
                    <option value={10}>10 / page</option>
                    <option value={25}>25 / page</option>
                    <option value={50}>50 / page</option>
                </select>
            </div>
        </div>
    );
}

export default SearchResultContainer;