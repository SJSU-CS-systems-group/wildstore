import { GoogleMap, useLoadScript, Marker, Polygon, Data } from '@react-google-maps/api';
import { useCallback } from 'react';

const MapContainer = ({ metadataRecords }) => {
  const { isLoaded } = useLoadScript({
    googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_API_KEY,
  })
  if (!isLoaded) return <div>Loading...</div>

  const customStyles = [
    { elementType: "geometry", stylers: [{ color: "#242f3e" }] },
    { elementType: "labels.text.stroke", stylers: [{ color: "#242f3e" }] },
    { elementType: "labels.text.fill", stylers: [{ color: "#746855" }] },
    {
      featureType: "administrative.locality",
      elementType: "labels.text.fill",
      stylers: [{ color: "#d59563" }],
    },
    {
      featureType: "poi",
      elementType: "labels.text.fill",
      stylers: [{ color: "#d59563" }],
    },
    {
      featureType: "poi.park",
      elementType: "geometry",
      stylers: [{ color: "#263c3f" }],
    },
    {
      featureType: "poi.park",
      elementType: "labels.text.fill",
      stylers: [{ color: "#6b9a76" }],
    },
    {
      featureType: "road",
      elementType: "geometry",
      stylers: [{ color: "#38414e" }],
    },
    {
      featureType: "road",
      elementType: "geometry.stroke",
      stylers: [{ color: "#212a37" }],
    },
    {
      featureType: "road",
      elementType: "labels.text.fill",
      stylers: [{ color: "#9ca5b3" }],
    },
    {
      featureType: "road.highway",
      elementType: "geometry",
      stylers: [{ color: "#746855" }],
    },
    {
      featureType: "road.highway",
      elementType: "geometry.stroke",
      stylers: [{ color: "#1f2835" }],
    },
    {
      featureType: "road.highway",
      elementType: "labels.text.fill",
      stylers: [{ color: "#f3d19c" }],
    },
    {
      featureType: "transit",
      elementType: "geometry",
      stylers: [{ color: "#2f3948" }],
    },
    {
      featureType: "transit.station",
      elementType: "labels.text.fill",
      stylers: [{ color: "#d59563" }],
    },
    {
      featureType: "water",
      elementType: "geometry",
      stylers: [{ color: "#17263c" }],
    },
    {
      featureType: "water",
      elementType: "labels.text.fill",
      stylers: [{ color: "#515c6d" }],
    },
    {
      featureType: "water",
      elementType: "labels.text.stroke",
      stylers: [{ color: "#17263c" }],
    },
  ];

  const position={ lat: 44, lng: -80 };

  const onLoad = map => {
    const outerCoords = [
      { lat: -30.364, lng: 153.207 },
      { lat: -39.364, lng: 153.207 },
      { lat: -39.364, lng: 168.207 },
      { lat: -30.364, lng: 168.207 }, // north east
    ];
  
    map.data.add({
      geometry: new Polygon([
        outerCoords,
      ]),
    });
    // map.data.loadGeoJson(
      // {
      //   "type": "FeatureCollection",
      //   "features": [
      //     {
      //       "type": "Feature",
      //       "properties": {
      //         "letter": "G",
      //         "color": "blue",
      //         "rank": "7",
      //         "ascii": "71"
      //       },
      //       "geometry": {
      //         "type": "Polygon",
      //         "coordinates": [
      //             [
      //                 [
      //                     -111.7889404296875,
      //                     38.20677185058594
      //                 ],
      //                 [
      //                     -111.7889404296875,
      //                     38.66585159301758
      //                 ],
      //                 [
      //                     -112.37643432617188,
      //                     38.66585159301758
      //                 ],
      //                 [
      //                     -112.37643432617188,
      //                     38.20677185058594
      //                 ],
      //                 [
      //                     -111.7889404296875,
      //                     38.20677185058594
      //                 ]
      //             ]
      //         ]
      //     }
      //     }
      //   ]
      // }
    //   "https://storage.googleapis.com/mapsdevsite/json/google.json"
    // );
  }

  return (
    <GoogleMap
      zoom={10}
      center={{ lat: 44, lng: -80 }}
      mapContainerStyle={{ width: "100%", height: "100%" }}
      onLoad={onLoad}
      // data={<Polygon path={}/>}
    // options={{styles: customStyles}}
    >
      <div className='flex flex-col items-center mt-3'>
        <button className='relative bg-slate-100 
                text-primary shadow-xl rounded-full px-3 py-2 
                content-center capitalize text-sm 
                font-bold textShadow-lg btn-effect'>Search this area</button>
      </div>
    </GoogleMap>
  );
}

export default MapContainer;