import SideBar from '../sidebar/sidebar';
import MapContainer from '../map-container/mapContainer';
import Workspace from '../workspace/workspace';
import Navbar from '../navbar/navbar';
import { Outlet } from 'react-router-dom';

const Layout = () => {
    return (
        <div className='flex flex-col w-screen h-screen absolute'>
            <Navbar />
            <div className='flex-1 grid grid-cols-[100px_repeat(12,_minmax(0,_1fr))]'>
                <div id="sidebar" className='basis-20 text-white bg-primary shadow-lg shadow-primary'>
                    <SideBar />
                </div>
                <Outlet />
            </div>
        </div>
    );
}

export default Layout;