import { GoPaperAirplane, GoDownload } from 'react-icons/go';
import Modal from '../modal/modal';

const SearchResult = ({ metadataRecord }) => {
    return (
        <div className="card bg-base-100 shadow-xl w-0 min-w-full hover:shadow-2xl">
            <div className="card-body">
                <h3 className="card-title text-lg hover:cursor-pointer">{metadataRecord.fileName[0]}</h3>
                <p className="break-words">{metadataRecord.filePath[0]}</p>
                <div className="card-actions justify-end items-center">
                    <div><Modal /></div>
                    <div className='grow'></div>
                    <button class="btn btn-square bg-transparent border-none">
                        <GoPaperAirplane size={20} />
                    </button>
                    <button class="btn btn-square bg-transparent border-none">
                        <GoDownload size={20} />
                    </button>
                </div>
            </div>
        </div>
    );
}

export default SearchResult;