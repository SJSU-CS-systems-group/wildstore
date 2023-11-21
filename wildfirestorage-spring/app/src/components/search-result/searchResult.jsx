import { GoPaperAirplane, GoDownload, GoLocation } from 'react-icons/go';
import Modal from '../modal/modal';
import { useDispatch } from 'react-redux';
import { setSelectedRecord } from '../../redux/mapSlice';
import { setModalMetadata } from '../../redux/metadataSlice';
import { useSelector } from 'react-redux';

const SearchResult = ({ metadataRecord, setShowModal }) => {

    const dispatch = useDispatch();

    const locateOnMap = () => {
        dispatch(setSelectedRecord(metadataRecord))
    }

    const getData = async () => {
        const response = await fetch("http://localhost:8080/api/metadata/search", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/html, application/json",
            },
            body: JSON.stringify({"searchQuery":`digestString = '${metadataRecord.digestString}'`}),
            credentials: "include",
            redirect: "follow",
        });
        if(response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        console.log("^^^^^^^^^^^", d[0], metadataRecord.digestString)
        dispatch(setModalMetadata(d[0]));
        console.log("data", d)
        // setRecords(d)
    }

    const fetchDetails = () => {
        setShowModal(true);
        getData();
    }

    const token = useSelector(state => state.userReducer.opaqueToken)

    const download = () => {
        fetch("http://localhost:100/api/file/FqBypAUpvL6GlPcx", {
            method: 'GET',
            headers: new Headers({
                'Authorization': 'Bearer ' + token
            }),
        })
        .then(function (response) { return response.blob();})
        .then(function (blob) {
            var url = window.URL.createObjectURL(blob);
            console.log(url);
            var a = document.createElement('a');
            a.href = url;
            a.download = metadataRecord.filePath[0];
            document.body.appendChild(a); // we need to append the element to the dom -> otherwise it will not work in firefox
            a.click();    
            a.remove();  //afterwards we remove the element again  
        });
        // const blob = await response.blob();
        // console.log("blob", blob);
        // var file = window.URL.createObjectURL(blob);
        // window.location.assign(file);
        // let d = await response.json();
    }

    return (
        <div className="card bg-base-100 shadow-xl w-0 min-w-full hover:shadow-2xl">
            <div className="card-body">
                <h3 className="card-title text-lg hover:cursor-pointer">{metadataRecord.fileName[0]}</h3>
                <p className="break-words">{metadataRecord.filePath[0]}</p>
                <div className="card-actions justify-end items-center">
                    <div><a className="link link-primary" onClick = {fetchDetails}>Details</a></div>
                    <div className='grow'></div>
                    <div className='tooltip' data-tip="Locate on Map">
                        <button className="btn btn-square bg-transparent border-none" onClick={locateOnMap}>
                            <GoLocation size={20} />
                        </button>
                    </div>
                    <div className='tooltip' data-tip="Send">
                        <button className="btn btn-square bg-transparent border-none">
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
        </div >
    );
}

export default SearchResult;