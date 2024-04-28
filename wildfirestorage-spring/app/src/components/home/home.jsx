import SideBar from '../sidebar/sidebar';
import MapContainer from '../map-container/mapContainer';
import Workspace from '../workspace/workspace';
import Navbar from '../navbar/navbar';

const Home = () => {
    return (
        <>
            <div id="workspace" className='col-span-5 shadow-lg' style={{ "height": `calc(100vh - 4rem)`, "overflowY": "scroll" }}>
                <Workspace />
            </div>
            <div id="map" className='col-span-7'>
                <MapContainer style={{ "height": `calc(100vh - 4rem)` }} />
            </div>
        </>
    );
}

export default Home;