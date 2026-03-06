import SideBar from '../sidebar/sidebar';
import MapView from '../map-view/mapView';
import Workspace from '../workspace/workspace';
import Navbar from '../navbar/navbar';

const Home = () => {
  return (
    <>
      <div id="workspace" className='col-span-5 shadow-lg h-[calc(100vh-4rem)] overflow-y-scroll'>
        <Workspace />
      </div>
      <div id="map" className='col-span-7 h-[calc(100vh-4rem)]'>
        <MapView />
      </div>
    </>
  );
}

export default Home;
