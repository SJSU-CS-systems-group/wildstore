import './App.css';
import SideBar from './components/sidebar/sidebar';
import MapContainer from './components/map-container/mapContainer';
import Navbar from './components/navbar/navbar';
import Workspace from './components/workspace/workspace';

function App() {

  return (
    <div className='flex flex-col w-screen h-screen absolute'>
      <Navbar />
      <div className='flex-1 grid grid-cols-[100px_repeat(12,_minmax(0,_1fr))]'>
        <div id="sidebar" className='basis-20 text-white bg-primary shadow-lg shadow-primary'>
          <SideBar />
        </div>
        <div id="workspace" className='col-span-4 shadow-lg' style={{"height": `calc(100vh - 4rem)`, "overflowY": "scroll"}}>
          <Workspace />
        </div>
        <div id="map" className='col-span-8'>
          <MapContainer style={{"height": `calc(100vh - 4rem)`}}/>
        </div>
      </div>
    </div>
  )
}

export default App;
