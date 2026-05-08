import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { useEffect, useMemo, useState } from 'react';
import { MapContainer, Marker, Polygon, Popup, TileLayer, useMap } from 'react-leaflet';
import { useDispatch, useSelector } from 'react-redux';
import { addQuery, setCurrentPage } from '../../redux/filterSlice';
import MetadataDetails from '../metadata-details/metadataDetails';

import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';

L.Icon.Default.mergeOptions({
  iconRetinaUrl,
  iconUrl,
  shadowUrl,
});

const EXTENT_THRESHOLD = 0.02; // If bbox smaller than this, treat as point

/**
 * Component to handle map interactions (Search Button)
 * We need this child component to access the 'map' instance via useMap()
 */
const MapController = () => {
  const map = useMap();
  const dispatch = useDispatch();

  useEffect(() => {
    const handleSearch = () => {
      const bounds = map.getBounds();
      const ne = bounds.getNorthEast();
      const sw = bounds.getSouthWest();

      const query = `LOCATION IN ((${ne.lat}, ${sw.lng}), (${ne.lat},${ne.lng}), (${sw.lat},${ne.lng}), (${sw.lat}, ${sw.lng}))`;

      dispatch(addQuery(query));
      dispatch(setCurrentPage(1));
    };
    const SearchControl = L.Control.extend({
      onAdd: () => {
        const button = L.DomUtil.create('button', 'leaflet-bar leaflet-control leaflet-control-custom');
        button.innerHTML = 'Search this area';
        button.style.backgroundColor = 'white';
        button.style.color = '#0056b3';
        button.style.padding = '10px 15px';
        button.style.border = '2px solid rgba(0,0,0,0.2)';
        button.style.borderRadius = '20px';
        button.style.cursor = 'pointer';
        button.style.fontSize = '14px';
        button.style.fontWeight = 'bold';
        button.style.boxShadow = '0 1px 5px rgba(0,0,0,0.65)';
        
        button.onclick = (e) => {
          L.DomEvent.stopPropagation(e); // Prevent map click
          handleSearch();
        };
        return button;
      }
    });

    const searchControl = new SearchControl({ position: 'topright' });
    map.addControl(searchControl);

    return () => {
      map.removeControl(searchControl);
    };
  }, [map, dispatch]);

  return null;
};

const LocateHandler = ({ setSelectedId }) => {
  const map = useMap();
  const selectedRecord = useSelector((state) => state.mapReducer.selectedRecord);
  const locateTimestamp = useSelector((state) => state.mapReducer.locateTimestamp);

  useEffect(() => {
    if (!selectedRecord?.digestString) return;

    const vars = selectedRecord.variables || [];
    const latVar = vars.find((v) => v.variableName === 'latitude' || v.variableName === 'XLAT');
    const lonVar = vars.find((v) => v.variableName === 'longitude' || v.variableName === 'XLONG');
    if (!latVar || !lonVar) return;

    const latMin = parseFloat(latVar.minValue);
    const latMax = parseFloat(latVar.maxValue);
    const lonMin = parseFloat(lonVar.minValue);
    const lonMax = parseFloat(lonVar.maxValue);
    if (!isFinite(latMin) || !isFinite(latMax) || !isFinite(lonMin) || !isFinite(lonMax)) return;

    const bounds = L.latLngBounds([latMin, lonMin], [latMax, lonMax]);
    map.flyToBounds(bounds, { padding: [50, 50], maxZoom: 10 });
    setSelectedId(selectedRecord.digestString);
  }, [locateTimestamp, map, setSelectedId]);

  return null;
};

const MapView = () => {
  const [selectedId, setSelectedId] = useState(null); // Which square has its popup open

  const metadataRecords = useSelector(
    (state) => state.metadataReducer.metadataRecords
  );

  // Build features based on min/max latitude/longitude
  const features = useMemo(() => {
    if (!metadataRecords || metadataRecords.length === 0) return [];

    return metadataRecords
      .map((record, idx) => {
        const vars = record.variables || [];

        const latVar =
          vars.find(
            (v) =>
              v.variableName === 'latitude' ||
              v.variableName === 'XLAT'
          ) || null;

        const lonVar =
          vars.find(
            (v) =>
              v.variableName === 'longitude' ||
              v.variableName === 'XLONG'
          ) || null;

        // If we have no lat/lon info, we can’t place it
        if (!latVar || !lonVar) return null;

        const latMin = parseFloat(latVar.minValue);
        const latMax = parseFloat(latVar.maxValue);
        const lonMin = parseFloat(lonVar.minValue);
        const lonMax = parseFloat(lonVar.maxValue);

        if (
          !isFinite(latMin) ||
          !isFinite(latMax) ||
          !isFinite(lonMin) ||
          !isFinite(lonMax)
        ) {
          return null;
        }

        const centerLat = (latMin + latMax) / 2;
        const centerLng = (lonMin + lonMax) / 2;

        const latSpan = Math.abs(latMax - latMin);
        const lonSpan = Math.abs(lonMax - lonMin);

        const id = record.digestString || idx;

        // If bbox big enough, draw a square
        // If too small, treat as point and only show a pin.
        if (latSpan >= EXTENT_THRESHOLD && lonSpan >= EXTENT_THRESHOLD) {
          const polygonLatLngs = [
            [latMin, lonMax],
            [latMax, lonMax],
            [latMax, lonMin],
            [latMin, lonMin],
            [latMin, lonMax],
          ];

          return {
            id,
            record,
            type: 'polygon',
            polygonLatLngs,
            markerPos: [centerLat, centerLng], 
          };
        } else {
          return {
            id,
            record,
            type: 'point',
            polygonLatLngs: null,
            markerPos: [centerLat, centerLng],
          };
        }
      })
      .filter(Boolean);
  }, [metadataRecords]);

  return (
    <div className="relative h-full w-full">
      <MapContainer
        center={[0, 0]}
        zoom={2}
        className="h-full w-full"
      >
        <TileLayer
          url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        />

        {features.map((feat) => {
          if (feat.type === 'polygon') {
            return (
              <Polygon
                key={feat.id}
                positions={feat.polygonLatLngs}
                pathOptions={{
                  color: selectedId === feat.id ? 'red' : 'gray',
                  weight: selectedId === feat.id ? 3 : 1,
                  fillOpacity: 0.2,
                }}
                eventHandlers={{
                  click: () => setSelectedId(feat.id),
                }}
              >
                <Popup
                  eventHandlers={{
                    remove: () => setSelectedId(null),
                  }}
                >
                  <MetadataDetails record={feat.record} />
                </Popup>
              </Polygon>
            );
          }

          // point-only fallback (no usable bbox)
          return (
            <Marker key={feat.id} position={feat.markerPos}>
              <Popup><MetadataDetails record={feat.record} /></Popup>
            </Marker>
          );
        })}
        <MapController />
        <LocateHandler setSelectedId={setSelectedId} />
      </MapContainer>
    </div>
  );
};

export default MapView;
