import './App.css';
import SideBar from './components/sidebar/sidebar';
import MapContainer from './components/map-container/mapContainer';
import Navbar from './components/navbar/navbar';
import Workspace from './components/workspace/workspace';
import { useEffect, useState } from 'react';

function App() {
  

  const [records, setRecords] = useState([]);

    const getData = async () => {
        const response = await fetch("http://localhost:8080/api/metadata/search", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              "Accept": "text/html, application/json",
            },
            body: JSON.stringify({"searchQuery":"VAR.NorthWind.minValue > 0.0", "excludeFields": ["variables", "globalAttributes"]}),
            credentials: "include",
            redirect: "follow",
        });
        if(response.redirected) {
          document.location = response.url;
      }
      console.log(response);
      let d = await response.json();
      console.log("data", d)
      setRecords(d)
    }

    useEffect(() => {
      getData();
  }, [])

  return (
    <div className='flex flex-col h-screen absolute'>
      <Navbar />
      <div className='flex-1 grid grid-cols-[100px_repeat(12,_minmax(0,_1fr))]'>
        <div id="sidebar" className='basis-20 text-white bg-primary shadow-lg shadow-primary'>
          <SideBar />
        </div>
        <div id="workspace" className='col-span-4 shadow-lg' style={{"height": `calc(100vh - 4rem)`, "overflowY": "scroll"}}>
          <Workspace />
        </div>
        <div id="map" className='col-span-8'>
          <MapContainer metadataRecords={records}/>
        </div>
      </div>
    </div>
  )
}

export default App;
