import { useEffect, useRef } from 'react';
import { centroid, feature, featureCollection, bbox } from '@turf/turf';
import { useSelector, useDispatch } from 'react-redux';
import { setMetadata } from '../../redux/metadataSlice';
import { setSelectedRecord } from '../../redux/mapSlice';
import { setQuery } from '../../redux/filterSlice';

const google = window.google;
let map;

const MapContainer = () => {

  const mapDiv = useRef(null);
  const dispatch = useDispatch();
  const metadataRecords = useSelector(state => state.metadataReducer.metadataRecords)
  const selectedRecord = useSelector(state => state.mapReducer.selectedRecord)


  console.log("********", metadataRecords)


  function createCenterControl(map) {
    const controlButton = document.createElement("button");
    controlButton.className = "mt-3 bg-slate-100 text-primary shadow-xl rounded-full px-3 py-2 content-center capitalize text-sm font-bold textShadow-lg btn-effect";
    controlButton.textContent = "Search this area";
    controlButton.type = "button";
    controlButton.addEventListener("click", search);
    return controlButton;
  }

  async function initMap() {
    const { Map } = await google.maps.importLibrary("maps");

    map = new Map(mapDiv.current, {
      center: { lat: 39.8283, lng: -98.5795 },
      zoom: 5,
    });
    const centerControlDiv = document.createElement("div");
    const centerControl = createCenterControl(map);
    centerControlDiv.appendChild(centerControl);
    map.controls[google.maps.ControlPosition.TOP_CENTER].push(centerControlDiv);
    map.data.setStyle({
      fillColor: "gray",
      strokeWeight: 1,
      fillOpacity: 0.2
    });
    map.data.addListener("mouseover", (event) => {
      map.data.revertStyle();
      map.data.overrideStyle(event.feature, { strokeWeight: 3 });
    });
    map.data.addListener("mouseout", (event) => {
      map.data.revertStyle();
    });
    refreshMapData();
    window.mymap = map;
  }

  const search = () => {
    let bounds = map.getBounds();
    let ne = bounds.getNorthEast();
    let sw = bounds.getSouthWest();

    let query = `LOCATION IN ((${ne.lat()}, ${sw.lng()}), (${ne.lat()},${ne.lng()}), (${sw.lat()},${ne.lng()}), (${sw.lat()}, ${sw.lng()}))`;
    console.log("query", query)

    dispatch(setQuery(query));
  }

  useEffect(() => {
    if (map) {
      refreshMapData();
    }
  }, [metadataRecords]);

  const refreshMapData = () => {
    map.data.forEach(feature => {
      map.data.remove(feature);
    })
    for (let record of metadataRecords) {
      let geoJsonFeature = feature(record.location);
      geoJsonFeature.properties = { "id": record.digestString }
      map.data.addGeoJson(geoJsonFeature);
      map.data.addGeoJson(centroid(record.location));
    }
  }

  useEffect(() => {
    initMap();
  }, [])

  useEffect(() => {
    if (map) {
      map.data.setStyle(feature => {
        if (feature.getProperty("id") === selectedRecord.digestString) {
          return {
            fillColor: "red"
          }
        } else {
          return {
            fillColor: "gray"
          }
        }
      })
    }
  }, [selectedRecord]);

  return (
    <div style={{ position: "relative" }}>
      <div ref={mapDiv} id="mymap" style={{ width: "100%", "height": `calc(100vh - 4rem)`, position: "absolute" }}></div>
    </div>
  );
}

export default MapContainer;