import SideBar from '../sidebar/sidebar';
import MapContainer from '../map-container/mapContainer';
import Workspace from '../workspace/workspace';
import Navbar from '../navbar/navbar';

const Home = () => {
    return (
        <div className='flex flex-col w-screen h-screen absolute'>
            <Navbar />
            <div className='flex-1 grid grid-cols-[100px_repeat(12,_minmax(0,_1fr))]'>
                <div id="sidebar" className='basis-20 text-white bg-primary shadow-lg shadow-primary'>
                    <SideBar />
                </div>
                <div id="workspace" className='col-span-5 shadow-lg' style={{ "height": `calc(100vh - 4rem)`, "overflowY": "scroll" }}>
                    <Workspace />
                </div>
                <div id="map" className='col-span-7'>
                    <MapContainer style={{ "height": `calc(100vh - 4rem)` }} />
                </div>
            </div>
        </div>
    );
}

export default Home;